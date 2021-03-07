package app

import java.nio.file.Paths

import app.services.{Discord, Slack, Twilio}
import org.http4s.HttpRoutes
import cats.implicits._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends App {

  private val dsl = Http4sDsl[Task]
  import dsl._

  def serve: URIO[zio.ZEnv, ExitCode] = {
    val configPath = Paths
      .get(getClass.getClassLoader.getResource("botschaft.json").toURI)
      .getParent


    def apiFrom[A](r: A => HttpRoutes[Task])(implicit tag: Tag[A]): ZIO[Has[Option[A]], Nothing, HttpRoutes[Task]] =
      ZIO.access[Has[Option[A]]]{ maybeService =>
        maybeService.get match {
          case Some(service) => r(service)
          case None => HttpRoutes.empty[Task]
        }
      }

    def apis: ZIO[Any, Exception, Seq[HttpRoutes[Task]]] = for {
      cfg <- ZIO.fromEither(config.loadConfig(configPath))
      slackDep = Slack.live(cfg.providers.slack)
      slackRoutes <- apiFrom(routes.slackApi).provideLayer(slackDep)
      discordDep = Discord.live(cfg.providers.discord)
      discordRoutes <- apiFrom(routes.discordApi).provideLayer(discordDep)
      twilioDep = Twilio.live(cfg.providers.twilio)
      twilioRoutes <- apiFrom(routes.twilioApi).provideLayer(twilioDep)
    } yield Seq(slackRoutes, discordRoutes, twilioRoutes)

    val httpApp = (apis: Seq[HttpRoutes[Task]]) =>
      apis
        .fold(HttpRoutes.empty[Task]) {
          case (app, routes) =>
            app <+> routes
        }
        .orNotFound

    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      apis.map(httpApp).flatMap { http =>
        BlazeServerBuilder[Task](scala.concurrent.ExecutionContext.global)
          .bindHttp(8000, "0.0.0.0")
          .withHttpApp(http)
          .serve
          .compile
          .drain
      }.foldM(_ => ZIO.succeed(ExitCode.failure), _ => ZIO.succeed(ExitCode.success))
    }
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    serve
}
