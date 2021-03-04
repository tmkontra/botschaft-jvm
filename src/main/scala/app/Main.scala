package app

import java.nio.file.Paths

import app.services.SlackApi.Service
import app.services.{HasSlackApi, SlackApi, SlackMessage}
import cats.implicits._
import config.BotschaftConfig.SlackConfig
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeServerBuilder
import zio._

object Main extends App {

  private val dsl = Http4sDsl[Task]

  def serve: URIO[zio.ZEnv, ExitCode] = {
    val configPath = Paths
      .get(getClass.getClassLoader.getResource("botschaft.json").toURI)
      .getParent

    def apis: Either[Exception, Seq[HttpRoutes[Task]]] = for {
      cfg <- config.loadConfig(configPath)
      discord = cfg.providers.discord.map(d => http.discordApi(d))
      twilio = cfg.providers.twilio.map(t => http.twilioApi(t))
    } yield Seq(discord, twilio).flatten

    val slack: ZIO[SlackApi.Service, Nothing, HttpRoutes[Task]] = for {
      api <- ZIO.accessM[SlackApi.Service](s => ZIO.succeed(http.slackApi(s)))
    } yield api

    val httpApp = (apis: Seq[HttpRoutes[Task]]) =>
      apis
        .fold(HttpRoutes.empty[Task]) {
          case (app, routes) =>
            app <+> routes
        }

    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      val api = apis.getOrElse(throw new Exception("could not load config"))
      val http = httpApp(api)
      val layer: ZLayer[Any, Nothing, Has[SlackApi.Service]] = ZLayer
        .succeed(SlackConfig(Map.empty)) >>> SlackApi.live
      val sa: ZIO[Any, Nothing, HttpRoutes[Task]] = slack.provide {
        new Service {
          override def sendMessage(message: SlackMessage): Task[String] =
            Task.fromEither {
              for {
                _ <- SlackConfig(Map.empty).channels.get(message.toChannel)
                  .toRight(new Exception(s"No such channel ${message.toChannel}"))
              } yield "sent!"
            }
        }
      }
      sa.flatMap { s =>
        BlazeServerBuilder[Task](scala.concurrent.ExecutionContext.global)
          .bindHttp(8000, "0.0.0.0")
          .withHttpApp((http <+> s).orNotFound)
          .serve
          .compile
          .drain
          .foldM(_ => ZIO.succeed(ExitCode.failure), _ => ZIO.succeed(ExitCode.success))
      }
    }

  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    serve
}
