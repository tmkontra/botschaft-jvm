package app

import zio.interop.catz._
import config.BotschaftConfig.{DiscordConfig, SlackConfig, TwilioConfig}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import services.{DiscordMessage, HasSlackApi, SlackApi, SlackMessage, TwilioMessage}
import zio.{Task, URIO, ZLayer}

object http extends Http4sDsl[Task] {

  object message extends QueryParamDecoderMatcher[String]("message")
  object channel extends QueryParamDecoderMatcher[String]("channel")

  def slackApi(slack: SlackApi.Service): HttpRoutes[Task] = {
    HttpRoutes
      .of[Task] {
        case GET -> Root / "slack" :? message(message) +& channel(channel) =>
          Ok(
            slack
              .sendMessage(SlackMessage(message, channel))
              .foldM(
                ex => Task.succeed(ex.getMessage),
                _ => Task.succeed(s"sent $message")
              )
          )
      }
  }

  def discordApi(discordConfig: DiscordConfig): HttpRoutes[Task] = {
    val discord = new services.DiscordApi(discordConfig)
    HttpRoutes
      .of[Task] {
        case GET -> Root / "discord" :? message(message) +& channel(channel) =>
          Ok(
            Task
              .fromEither(discord.sendMessage(DiscordMessage(message, channel)))
              .foldM(
                ex => Task.succeed(ex.getMessage),
                _ => Task.succeed(s"sent $message")
              )
          )
      }
  }

  object to extends QueryParamDecoderMatcher[String]("to")

  def twilioApi(twilioConfig: TwilioConfig): HttpRoutes[Task] = {
    val twilio = new services.TwilioApi(twilioConfig)
    HttpRoutes
      .of[Task] {
        case GET -> Root / "twilio" :? message(message) +& to(to) =>
          Ok(
            Task
              .fromEither(twilio.sendMessage(TwilioMessage(message, to)))
              .foldM(
                ex => Task.succeed(ex.getMessage),
                _ => Task.succeed(s"sent $message")
              )
          )
      }
  }
}
