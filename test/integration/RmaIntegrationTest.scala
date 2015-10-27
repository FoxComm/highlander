import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models._
import responses.RmaResponse
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.time._
import utils.Slick.implicits._
import scala.concurrent.ExecutionContext.Implicits.global

class RmaIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GET /v1/rmas/:code" - {
    "should return valid RMA by code" in new Fixture {
      val response = GET(s"v1/rmas/${rma.refNum}")
      response.status must === (StatusCodes.OK)

      val root = response.as[RmaResponse.Root]
      root.referenceNumber mustBe rma.refNum
    }

    "should return 404 if invalid rma is returned" in new Fixture {
      val response = GET(s"v1/rmas/ABC-666")
      response.status must === (StatusCodes.NotFound)
      response.errors must === (NotFoundFailure404(Rma, "ABC-666").description)
    }
  }

  trait Fixture {
    val (admin, rma) = (for {
      admin ← StoreAdmins.save(authedStoreAdmin)
      order ← Orders.save(Factories.order.copy(
        status = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
      rma ← Rmas.save(Factories.rma.copy(
        orderId = order.id,
        orderRefNum = order.referenceNumber,
        storeAdminId = Some(admin.id)))
    } yield (admin, rma)).run().futureValue
  }

}
