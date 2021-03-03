import config.BotschaftConfig.SlackConfig

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
}
