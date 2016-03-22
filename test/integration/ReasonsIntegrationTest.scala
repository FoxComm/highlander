import Extensions._
import akka.http.scaladsl.model.StatusCodes

import models.rma.{RmaReason, RmaReasons}
import models.{Reason, Reasons, StoreAdmins}
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Strings._
import utils.seeds.Seeds.Factories
import scala.concurrent.ExecutionContext.Implicits.global

import failures.InvalidReasonTypeFailure

class ReasonsIntegrationTest extends IntegrationTestBase
with HttpSupport
with AutomaticAuth {

  "Reasons" - {
    "GET /v1/public/reasons" - {
      "should return list of reasons" in new Fixture {
        val response = GET(s"v1/public/reasons")
        response.status must ===(StatusCodes.OK)

        val root = response.ignoreFailuresAndGiveMe[Seq[Reason]]
        root.size must ===(1)
        root.headOption.value.id must ===(reason.id)
      }
    }

    "GET /v1/public/reasons/:type" - {
      "should return list of reasons by type" in new Fixture {
        val reasonType = Reason.GiftCardCreation.toString.lowerCaseFirstLetter
        val response = GET(s"v1/public/reasons/$reasonType")
        response.status must ===(StatusCodes.OK)

        val root = response.ignoreFailuresAndGiveMe[Seq[Reason]]
        root.size must ===(1)
        root.headOption.value.id must ===(reason.id)
      }

      "should return error if invalid type provided" in new Fixture {
        val reasonType = "lolwut"
        val response = GET(s"v1/public/reasons/$reasonType")
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(InvalidReasonTypeFailure(reasonType).description)
      }
    }
  }

  "RmaReasons" - {
    "GET /v1/public/rma-reasons" - {
      "should return list of RMA reasons" in new Fixture {
        val response = GET(s"v1/public/rma-reasons")
        response.status must ===(StatusCodes.OK)

        val root = response.ignoreFailuresAndGiveMe[Seq[RmaReason]]
        root.size must ===(1)
        root.headOption.value.id must ===(rmaReason.id)
      }
    }
  }

  trait Fixture {
    val (reason, rmaReason) = (for {
      storeAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason ← * <~ Reasons.create(Factories.reasons.head.copy(reasonType = Reason.GiftCardCreation,
        storeAdminId = storeAdmin.id))
      rmaReason ← * <~ RmaReasons.create(Factories.rmaReasons.head)
    } yield (reason, rmaReason)).runTxn().futureValue.rightVal
  }
}
