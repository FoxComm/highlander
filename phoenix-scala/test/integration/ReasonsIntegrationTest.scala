import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.InvalidReasonTypeFailure
import models.returns.ReturnReasons
import models.{Reason, Reasons, StoreAdmins}
import util.IntegrationTestBase
import utils.Strings._
import utils.db._
import utils.seeds.Seeds.Factories

class ReasonsIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "Reasons" - {

    "GET /v1/public/reasons/:type" - {
      "should return list of reasons by type" in new Fixture {
        val reasonType = Reason.GiftCardCreation.toString.lowerCaseFirstLetter
        val response   = GET(s"v1/public/reasons/$reasonType")
        response.status must === (StatusCodes.OK)

        val root = response.as[Seq[Reason]]
        root.size must === (1)
        root.headOption.value.id must === (reason.id)
      }

      "should return error if invalid type provided" in new Fixture {
        val reasonType = "lolwut"
        val response   = GET(s"v1/public/reasons/$reasonType")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (InvalidReasonTypeFailure(reasonType).description)
      }
    }
  }

  trait Fixture {
    val (reason, returnReason) = (for {
      storeAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason ← * <~ Reasons.create(
                  Factories.reasons.head.copy(reasonType = Reason.GiftCardCreation,
                                              storeAdminId = storeAdmin.id))
      returnReason ← * <~ ReturnReasons.create(Factories.returnReasons.head)
    } yield (reason, returnReason)).gimme
  }
}
