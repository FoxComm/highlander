package payloads

import models.channel.Channel.Location

object ChannelPayloads {
  case class CreateChannelPayload(scope: Option[String] = None,
                                  contextId: Option[Int] = None,
                                  location: Location,
                                  name: String)

  case class UpdateChannelPayload(name: String)
}
