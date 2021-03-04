package app

object providers {
  sealed trait Provider
  object Provider {
    case object Slack extends Provider
    case object Discord extends Provider
    case object Twilio extends Provider
    sealed trait AwsProvider extends Provider
    case object SNS extends AwsProvider
  }
}
