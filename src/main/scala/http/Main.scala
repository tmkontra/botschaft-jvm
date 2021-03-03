package http

import java.nio.file.Paths

import cats._
import cats.implicits._
import loader.load
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import zio._
import zio.duration._
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.dsl.Http4sDsl

import scala.concurrent.duration._

object Main extends App {

  private val dsl = Http4sDsl[Task]
  import dsl._

  def serve: URIO[zio.ZEnv, ExitCode] = {
    val configPath = Paths
      .get(getClass.getClassLoader.getResource("botschaft.json").toURI)
      .getParent
    val slack = for {
      cfg <- config.loadConfig(configPath)
      slack <- cfg.providers.slack.map(s => http.slackApi(s)).toRight(new Exception("no slack!"))
    } yield slack

    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      BlazeServerBuilder[Task](scala.concurrent.ExecutionContext.global)
        .bindHttp(8000, "0.0.0.0")
        .withHttpApp(slack.getOrElse(throw new Exception("ouch")).orNotFound)
        .serve
        .compile
        .drain
        .foldM(_ => ZIO.succeed(ExitCode.failure), _ => ZIO.succeed(ExitCode.success))
    }
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    serve
}
