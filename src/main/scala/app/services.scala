package app

import config.BotschaftConfig.{DiscordConfig, SlackConfig, TwilioConfig}
import zio.{Has, Task, UIO, URIO, ZIO, ZLayer}

object services {

  case class SlackMessage(message: String, toChannel: String)

  object Slack {

    trait Service {
      def sendMessage(message: SlackMessage): Task[String]
    }

    def live(slackConfig: Option[SlackConfig]): ZLayer[Any, Nothing, Has[Option[Slack.Service]]] =
      ZLayer.succeed {
        slackConfig.map { slackConfig =>
          new Service {
            override def sendMessage(message: SlackMessage): Task[String] =
              Task.fromEither {
                for {
                  _ <- slackConfig.channels.get(message.toChannel)
                    .toRight(new Exception(s"No such channel ${message.toChannel}"))
                } yield "sent!"
              }
          }
        }
      }
  }

  case class DiscordMessage(message: String, toChannel: String)

  object Discord {

    trait Service {
      def sendMessage(message: DiscordMessage): Either[Exception, Unit]
    }

    def live(discordConfig: Option[DiscordConfig]): ZLayer[Any, Nothing, Has[Option[Discord.Service]]] =
      ZLayer.succeed {
        discordConfig.map { discordConfig =>
          new Service {
            override def sendMessage(message: DiscordMessage): Either[Exception, Unit] =
              for {
                _ <- discordConfig.channels.get(message.toChannel)
                  .toRight(new IllegalArgumentException(s"No such channel ${message.toChannel}"))
                _ <- Right("sent!")
              } yield ()
          }
        }
      }
  }

  case class TwilioMessage(message: String, to: String)

  object Twilio {

    trait Service {
      def sendMessage(message: TwilioMessage): Either[Exception, Unit]
    }

    def live(twilioConfig: Option[TwilioConfig]): ZLayer[Any, Nothing, Has[Option[Twilio.Service]]] =
      ZLayer.succeed {
        twilioConfig.map { twilioConfig =>
          new Service {
            override def sendMessage(message: TwilioMessage): Either[Exception, Unit] =
              for {
                _ <- Right("sent!")
                _ = println(twilioConfig.toString)
              } yield ()
          }
        }
      }
  }

}
