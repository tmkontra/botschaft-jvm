package app

import config.BotschaftConfig.{DiscordConfig, SlackConfig, TwilioConfig}
import zio.{Has, Task, UIO, URIO, ZIO, ZLayer}

object services {
  case class SlackMessage(message: String, toChannel: String)

  type HasSlackApi = Has[SlackApi.Service]
  object SlackApi {
    trait Service {
      def sendMessage(message: SlackMessage): Task[String]
    }

    val live: ZLayer[Has[SlackConfig], Nothing, Has[SlackApi.Service]] =
      ZLayer.fromFunction { slackConfig =>
        new Service {
          override def sendMessage(message: SlackMessage): Task[String] =
            Task.fromEither {
              for {
                _ <- slackConfig.get.channels.get(message.toChannel)
                  .toRight(new Exception(s"No such channel ${message.toChannel}"))
              } yield "sent!"
            }
        }
      }
  }

  case class DiscordMessage(message: String, toChannel: String)
  class DiscordApi(config: DiscordConfig) {
    def sendMessage(message: DiscordMessage): Either[Exception, Unit] =
      for {
        _ <- config.channels.get(message.toChannel)
          .toRight(new IllegalArgumentException(s"No such channel ${message.toChannel}"))
        _ <- Right("sent!")
      } yield ()
  }

  case class TwilioMessage(message: String, to: String)
  class TwilioApi(twilioConfig: TwilioConfig) {
    def sendMessage(message: TwilioMessage): Either[Exception, Unit] =
      for {
        _ <- Right("sent!")
      } yield ()
  }


}
