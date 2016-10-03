import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.InvalidReasonTypeFailure
import models.returns.ReturnReasons
import models.{Reason, Reasons}
import util._
import util.fixtures.BakedFixtures
import utils.Strings._
import utils.db._
import utils.seeds.Seeds.Factories

class ReasonsIntegrationTest
    extends IntegrationTestBase
    with PhoenixPublicApi
    with AutomaticAuth
    with BakedFixtures {

  "Reasons" - {

    "GET /v1/public/reasons/:type" - {
      "should return list of reasons by type" in new Fixture {
        val reasonType = Reason.GiftCardCreation.toString.lowerCaseFirstLetter

        val root = publicApi.getReason(reasonType).as[Seq[Reason]]
        root.size must === (1)
        root.headOption.value.id must === (reason.id)
      }

      "should return error if invalid type provided" in new Fixture {
        publicApi.getReason("lolwut").mustFailWith400(InvalidReasonTypeFailure("lolwut"))
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val (reason, returnReason) = (for {
      reason ← * <~ Reasons.create(
                  Factories.reasons.head.copy(reasonType = Reason.GiftCardCreation,
                                              storeAdminId = storeAdmin.id))
      returnReason ← * <~ ReturnReasons.create(Factories.returnReasons.head)
    } yield (reason, returnReason)).gimme
  }
}
