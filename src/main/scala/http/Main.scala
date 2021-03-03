package http

import java.nio.file.Paths

import cats.SemigroupK
import cats.data.Kleisli
import cats.instances.all._
import cats.implicits._
import cats.data.Kleisli._
import cats.syntax.SemigroupKSyntax
import org.http4s._
import org.http4s.dsl._
import org.http4s.syntax.kleisli._
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends App with SemigroupKSyntax {

  private val dsl = Http4sDsl[Task]
  import dsl._

  implicitly[SemigroupK[HttpRoutes]]

  def combine(first: Routes, second: Routes) =
    first.combineK(second)

  def serve: URIO[zio.ZEnv, ExitCode] = {
    val configPath = Paths
      .get(getClass.getClassLoader.getResource("botschaft.json").toURI)
      .getParent
    val apis1 = for {
      cfg <- config.loadConfig(configPath)
      slack <- cfg.providers.slack.map(s => http.slackApi(s)).toRight(new Exception("no slack!"))
      discord <- cfg.providers.discord.map(d => http.discordApi(d)).toRight(new Exception("no discord!"))
    } yield Seq(slack, discord)

    val apis: List[HttpRoutes[Task]] = ???

    val app = apis
      .fold(HttpRoutes.empty[Task]){
        case (app: HttpRoutes[Task], routes: HttpRoutes[Task]) => app.combineK(routes)
      }.orNotFound

    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      BlazeServerBuilder[Task](scala.concurrent.ExecutionContext.global)
        .bindHttp(8000, "0.0.0.0")
        .withHttpApp(HttpRoutes.empty[Task].orNotFound)
        .serve
        .compile
        .drain
        .foldM(_ => ZIO.succeed(ExitCode.failure), _ => ZIO.succeed(ExitCode.success))
    }
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    serve
}
