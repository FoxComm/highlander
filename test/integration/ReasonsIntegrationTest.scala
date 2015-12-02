import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.ExecutionContext.Implicits.global

import Extensions._
import models._
import responses.ResponseWithFailuresAndMetadata
import services.InvalidReasonTypeFailure
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import DbResultT.implicits._
import util.IntegrationTestBase
import utils.seeds.Seeds.Factories
import utils.Slick.implicits._
import utils.Strings._

class ReasonsIntegrationTest extends IntegrationTestBase
with HttpSupport
with AutomaticAuth {

  "Reasons" - {
    "GET /v1/reasons" - {
      "should return list of reasons" in new Fixture {
        val response = GET(s"v1/reasons")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[Reason]]]
        root.result.size must ===(1)
        root.result.headOption.value.id must ===(reason.id)
      }
    }

    "GET /v1/reasons/:type" - {
      "should return list of reasons by type" in new Fixture {
        val reasonType = Reason.GiftCardCreation.toString.lowerCaseFirstLetter
        val response = GET(s"v1/reasons/$reasonType")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[Reason]]]
        root.result.size must ===(1)
        root.result.headOption.value.id must ===(reason.id)
      }

      "should return error if invalid type provided" in new Fixture {
        val reasonType = "lolwut"
        val response = GET(s"v1/reasons/$reasonType")
        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(InvalidReasonTypeFailure(reasonType).description)
      }
    }
  }

  "RmaReasons" - {
    "GET /v1/rma-reasons" - {
      "should return list of RMA reasons" in new Fixture {
        val response = GET(s"v1/rma-reasons")
        response.status must ===(StatusCodes.OK)

        val root = response.as[ResponseWithFailuresAndMetadata[Seq[RmaReason]]]
        root.result.size must ===(1)
        root.result.headOption.value.id must ===(rmaReason.id)
      }
    }
  }

  trait Fixture {
    val (reason, rmaReason) = (for {
      storeAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason ← * <~ Reasons.create(Factories.reasons.head.copy(reasonType = Reason.GiftCardCreation,
        storeAdminId = storeAdmin.id))
      rmaReason ← * <~ RmaReasons.create(Factories.rmaReasons.head)
    } yield (reason, rmaReason)).runT(txn = false).futureValue.rightVal
  }
}
