package responses

import models.channel._
import models.objects._
import ObjectResponses.ObjectContextResponse

object ChannelResponse {
  case class Root(id: Int,
                  name: String,
                  defaultContext: ObjectContextResponse.Root,
                  draftContext: ObjectContextResponse.Root)

  def build(channel: Channel, defaultContext: ObjectContext, draftContext: ObjectContext): Root =
    Root(id = channel.id,
         name = channel.name,
         defaultContext = ObjectContextResponse.build(defaultContext),
         draftContext = ObjectContextResponse.build(draftContext))
}
