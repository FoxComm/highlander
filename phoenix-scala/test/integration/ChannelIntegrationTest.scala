import java.time.Instant

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import cats.implicits._
import models.account.Scope
import models.channel._
import org.json4s.JsonDSL._
import org.json4s._
import responses.ChannelResponse
import payloads.ChannelPayloads._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures
import utils.aliases._
import utils.db._
import utils.time.RichInstant

class ChannelIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures
    with ApiFixtures {

  "POST v1/channels" - {
    "creates a new channel" in new StoreAdmin_Seed {
      val payload = CreateChannelPayload(scope = Some("1"),
                                         contextId = Some(ctx.id),
                                         location = Channel.Remote,
                                         name = "FooBarBaz")
      val root    = channelsApi.create(payload).as[ChannelResponse.Root]
      val created = Channels.mustFindActive404(root.id).gimme
      created.id must === (root.id)
      created.name must === (payload.name)
    }
  }

  "GET v1/channels/:channelId" - {
    "returns a created channel" in new Channel_Baked {
      val channelResp = channelsApi(channel.id).get.as[ChannelResponse.Root]
      channelResp.name must === ("Default")
    }
  }

  "PATCH /v1/channels/:channelId" - {
    "update existing channel" in new Channel_Baked {
      val updPayload = UpdateChannelPayload(name = "ARGHHHH")
      val updated    = channelsApi(channel.id).update(updPayload).as[ChannelResponse.Root]
      updated.name must === (updPayload.name)
    }
  }

  "DELETE /v1/channels/:channelId" - {
    "archives existing channel" in new Channel_Baked {
      channelsApi(channel.id).delete.mustBeEmpty()
      val deleted = Channels.findOneById(channel.id).gimme.value
      deleted.archivedAt mustBe 'defined
    }
  }
}
