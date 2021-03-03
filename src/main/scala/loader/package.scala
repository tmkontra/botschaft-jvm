import cats.data.NonEmptyList
import cats.implicits._
import config.BotschaftConfig
import config.BotschaftConfig.ProvidersConfig
import providers.Provider

package object loader  {

  case class APIs(available: NonEmptyList[Provider])
  object APIs {
    def fromConfig(available: List[Provider]): Option[APIs] =
      NonEmptyList.fromList(available).map(APIs.apply)
  }

  def load(config: BotschaftConfig): Option[APIs] = {
    val ProvidersConfig(slack, discord, twilio, aws) = config.providers
    val a = List(
        slack.map(_ => Provider.Slack),
        discord.map(_ => Provider.Discord),
        twilio.map(t => Provider.Twilio),
        aws.map(_ => Provider.SNS)
      ).flatten
    APIs.fromConfig(a)
  }
}
