import config.BotschaftConfig._

package object services {
  case class SlackMessage(message: String, toChannel: String)
  class SlackApi(slackConfig: SlackConfig) {
    def sendMessage(message: SlackMessage): Either[Exception, Unit] =
      for {
        channel <- slackConfig.channels.get(message.toChannel)
          .toRight(new Exception(s"No such channel ${message.toChannel}"))
        sent <- Right("sent!")
      } yield ()
  }

  case class DiscordMessage(message: String, toChannel: String)
  class DiscordApi(config: DiscordConfig) {
    def sendMessage(message: DiscordMessage): Either[Exception, Unit] =
      for {
        channel <- config.channels.get(message.toChannel)
          .toRight(new IllegalArgumentException(s"No such channel ${message.toChannel}"))
        sent <- Right("sent!")
      } yield ()
  }

  case class TwilioMessage(message: String, to: String)
  class TwilioApi(twilioConfig: TwilioConfig) {
    def sendMessage(message: TwilioMessage): Either[Exception, Unit] =
      ???
  }


}
