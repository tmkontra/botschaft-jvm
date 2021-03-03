package config

import cats.data.NonEmptyList
import config.BotschaftConfig._
import io.circe.Decoder.Result
import io.circe.{Decoder, Error, HCursor, JsonObject}
import io.circe.parser.decode

object json {
  private implicit val channels = new Decoder[Map[String, String]] {
    override def apply(c: HCursor): Result[Map[String, String]] =
      c.downField("channels").as[Map[String, String]]
  }
  private implicit val slack: Decoder[SlackConfig] = channels.map(SlackConfig)
  private implicit val discord: Decoder[DiscordConfig] = channels.map(DiscordConfig)
  private implicit val twilio: Decoder[TwilioConfig] = new Decoder[TwilioConfig] {
    override def apply(c: HCursor): Result[TwilioConfig] =
      for {
        accountSid <- c.downField("account_sid").as[String]
        authToken <- c.downField("auth_token").as[String]
        from <- c.downField("messaging_service_id").as[String]
          .map(TwilioFromId.MessagingServiceSid)
          .orElse {
            c.downField("from_phone_number").as[String]
              .map(TwilioFromId.FromPhoneNumber)
          }
      } yield TwilioConfig(accountSid, authToken, from)
  }
  private implicit val sns = new Decoder[SnsConfig] {
    override def apply(c: HCursor): Result[SnsConfig] =
      for {
        topicArn <- c.downField("topic_arn").as[String]
      } yield SnsConfig(topicArn)
  }
  private implicit val aws = new Decoder[AwsConfig] {
    override def apply(c: HCursor): Result[AwsConfig] =
      for {
        accessKeyId <- c.downField("access_key_id").as[String]
        secretAccessKey <- c.downField("secret_access_key").as[String]
        region <- c.downField("region").as[String]
        sns <- c.downField("sns").as[Option[SnsConfig]]
      } yield AwsConfig(accessKeyId, secretAccessKey, region, sns)
  }
  private implicit val p = new Decoder[ProvidersConfig] {
    override final def apply(c: HCursor): Result[ProvidersConfig] =
      for {
        s <- c.downField("slack").as[Option[SlackConfig]]
        d <- c.downField("discord").as[Option[DiscordConfig]]
        t <- c.downField("twilio").as[Option[TwilioConfig]]
        a <- c.downField("aws").as[Option[AwsConfig]]
      } yield ProvidersConfig(s, d, t, a)
  }
  private implicit val t = new Decoder[TopicConfig] {
    override final def apply(c: HCursor): Result[TopicConfig] =
      for {
        name <- c.downField("name").as[String]
        destinations <- c.downField("destinations").as[NonEmptyList[String]]
      } yield TopicConfig(name, destinations)
  }
  private implicit val d: Decoder[BotschaftConfig] = new Decoder[BotschaftConfig] {
    override final def apply(c: HCursor): Result[BotschaftConfig] =
      for {
        p <- c.downField("providers").as[ProvidersConfig]
        t <- c.downField("topics").as[Option[List[TopicConfig]]]
      } yield {
        BotschaftConfig(p, t.getOrElse(List.empty))
      }
  }

  def parseJson(json: String): Either[Error, BotschaftConfig] =
    decode[BotschaftConfig](json)
}
