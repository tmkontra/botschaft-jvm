import cats.implicits._
import config.BotschaftConfig
import config.BotschaftConfig.SlackConfig
import org.http4s.HttpRoutes
import zio._
import zio.interop.catz._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import services.SlackMessage

package object http extends Http4sDsl[Task] {

  object message extends QueryParamDecoderMatcher[String]("message")
  object channel extends QueryParamDecoderMatcher[String]("channel")

  def slackApi(slackConfig: SlackConfig): HttpRoutes[Task] = {
    val slack = new services.SlackApi(slackConfig)
    HttpRoutes
      .of[Task] {
        case GET -> Root / "slack" :? message(message) +& channel(channel) =>
          Ok(Task.succeed(
            slack.sendMessage(SlackMessage(message, channel))
          ).void)
      }
  }
}
