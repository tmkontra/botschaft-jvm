import java.io.File
import java.net.URI
import java.nio.file.{Path, Paths}

import cats.data.NonEmptyList
import config.BotschaftConfig._
import http.Main.getClass

import scala.io.Source
import scala.util.{Try, Using}

package object config {

  case class BotschaftConfig(providers: ProvidersConfig, topics: List[TopicConfig])

  object BotschaftConfig {
    type Channels = Map[String, String]

    case class ProvidersConfig(
                                slack: Option[SlackConfig],
                                discord: Option[DiscordConfig],
                                twilio: Option[TwilioConfig],
                                aws: Option[AwsConfig]
                              )
    case class SlackConfig(channels: Channels)
    case class DiscordConfig(channels: Channels)
    case class TwilioConfig(accountSid: String, authToken: String, from: TwilioFromId)
    sealed trait TwilioFromId
    object TwilioFromId {
      case class MessagingServiceSid(sid: String) extends TwilioFromId
      case class FromPhoneNumber(phoneNumber: String) extends TwilioFromId
    }
    case class AwsConfig(
                          accessKeyId: String,
                          secretAccessKey: String,
                          region: String,
                          snsConfig: Option[SnsConfig]
                        )
    case class SnsConfig(topicArn: String)

    case class TopicConfig(name: String, destinations: NonEmptyList[String])
  }

  private val configFormats: Seq[(String, String => Either[Exception, BotschaftConfig])] = List(
    (".json", json.parseJson),
    (".yaml", _ => Left(new Exception("Unsupported config format!"))),
    (".toml", _ => Left(new Exception("Unsupported config format!")))
  )

  def loadConfig(path: Path): Either[Exception, BotschaftConfig] =
    configFormats
      .iterator
      .map { case (ext, loader) =>
        println(s"Looking for ${ext}")
        val dir = new File(path.toUri)
        val f = new File(dir,s"botschaft$ext")
        Using(Source.fromFile(f, "utf-8"))(_.getLines().mkString("\n"))
          .map(file => (file, loader))
          .toEither
      }
      .collectFirst { case Right(config) =>
        config
      }
      .toRight(new Exception("No config file found!"))
      .flatMap {
        case (file, loader) => loader(file)
      }

}
