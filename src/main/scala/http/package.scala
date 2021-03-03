
import config.BotschaftConfig
import config.BotschaftConfig._
import org.http4s.HttpRoutes
import zio._
import zio.interop.catz._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import services._

package object http extends Http4sDsl[Task] {

  type Routes = HttpRoutes[Task]

  object message extends QueryParamDecoderMatcher[String]("message")
  object channel extends QueryParamDecoderMatcher[String]("channel")

  def slackApi(slackConfig: SlackConfig): Routes = {
    val slack = new services.SlackApi(slackConfig)
    HttpRoutes
      .of[Task] {
        case GET -> Root / "slack" :? message(message) +& channel(channel) =>
          Ok(
            Task
              .succeed(slack.sendMessage(SlackMessage(message, channel)))
              .as(s"sent ${message}")
          )
      }
  }

  def discordApi(discordConfig: DiscordConfig): Routes = {
    val discord = new services.DiscordApi(discordConfig)
    HttpRoutes
      .of[Task] {
        case GET -> Root / "discord" :? message(message) +& channel(channel) =>
          Ok(
            Task
              .succeed(discord.sendMessage(DiscordMessage(message, channel)))
              .as(s"sent ${message}")
          )
      }
  }

  def twilioApi(twilioConfig: TwilioConfig): Routes = {
    val twilio = new services.TwilioApi(twilioConfig)
    HttpRoutes
      .of[Task] {
        case GET -> Root / "discord" :? message(message) +& channel(channel) =>
          Ok(
            Task
              .succeed(???)
              .as(s"sent ${message}")
          )
      }
  }
}
