package app

import app.services._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.Task
import zio.interop.catz._
import zio.interop.catz.implicits._

object routes extends Http4sDsl[Task] {

  object message extends QueryParamDecoderMatcher[String]("message")
  object channel extends QueryParamDecoderMatcher[String]("channel")

  def slackApi(slack: Slack.Service): HttpRoutes[Task] = {
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

  def discordApi(discord: Discord.Service): HttpRoutes[Task] = {
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

  def twilioApi(twilio: Twilio.Service): HttpRoutes[Task] = {
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
