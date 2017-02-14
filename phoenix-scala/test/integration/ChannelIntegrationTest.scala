import java.time.Instant

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

import cats.implicits._
import models.account.Scope
import org.json4s.JsonDSL._
import org.json4s._
import responses.ChannelResponse
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
    with AutomaticAuth
    with BakedFixtures
    with ApiFixtures {

  "GET v1/channels" - {
    "returns a created channel" in new Channel_Baked {
      val channelResp = channelsApi(channel.id).get.as[ChannelResponse.Root]
      channelResp.name must === ("Default")
    }
  }
}
