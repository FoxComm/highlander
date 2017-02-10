package payloads

object ChannelPayloads {
  case class CreateChannelPayload(scope: Option[String] = None,
                                  contextId: Option[Int] = None,
                                  name: String)
  case class UpdateChannelPayload(contextId: Option[Int] = None, name: Option[String] = None)
}
