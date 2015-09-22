import akka.http.scaladsl.model.StatusCodes

import models.{PaymentMethod, OrderPayments, Orders, StoreCreditManuals, Customer, Reasons, Customers, StoreCredit,
StoreCredits, StoreAdmins}
import responses.{GiftCardAdjustmentsResponse, StoreCreditResponse, StoreCreditAdjustmentsResponse}
import org.scalatest.BeforeAndAfterEach
import services.NotFoundFailure
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class StoreCreditIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "StoreCredits" - {
    "POST /v1/customers/:id/payment-methods/store-credit" - {
      "when successful" - {
        "responds with the new storeCredit" in new Fixture {
          val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = scReason.id)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)
          val sc = response.as[responses.StoreCreditResponse.Root]

          response.status must === (StatusCodes.OK)
          sc.status must === (StoreCredit.Active)
        }
      }

      "fails if the customer is not found" in {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = 1)
        val response = POST(s"v1/customers/99/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.errors must === (NotFoundFailure(Customer, 99).description)
      }
    }

    "GET /v1/customers/:id/payment-methods/store-credit" - {
      "returns list of store credits" in new Fixture {
        val response = GET(s"v1/customers/${customer.id}/payment-methods/store-credit")
        val storeCredits = Seq(storeCredit)

        response.status must ===(StatusCodes.OK)
        val credits = response.as[Seq[StoreCredit]]
        credits.map(_.id) must ===(storeCredits.map(_.id))
      }

      "returns store credit by ID" in new Fixture {
        val response = GET(s"v1/store-credits/${storeCredit.id}")
        val storeCreditResponse = response.as[StoreCreditResponse.Root]

        response.status must ===(StatusCodes.OK)
        storeCreditResponse.availableBalance mustBe 40
      }

      "returns not found when SC doesn't exist" in new Fixture {
        val notFoundResponse = GET(s"v1/store-credits/99")
        notFoundResponse.status must ===(StatusCodes.NotFound)
        notFoundResponse.errors.head mustBe "storeCredit with id=99 not found"
      }
    }

    "GET /v1/store-credits/:id/transactions" - {
      "returns the list of adjustments" in new Fixture {
        val response = GET(s"v1/store-credits/${storeCredit.id}/transactions")
        val adjustments = response.as[Seq[StoreCreditAdjustmentsResponse.Root]]

        response.status must ===(StatusCodes.OK)
        adjustments.size mustBe 1

        val firstAdjustment = adjustments.head
        firstAdjustment.debit mustBe 10
        firstAdjustment.orderRef.get mustBe order.referenceNumber
      }
    }
  }

  trait Fixture {
    val (admin, customer, scReason, storeCredit, order) = (for {
      admin       ← StoreAdmins.save(authedStoreAdmin)
      customer    ← Customers.save(Factories.customer)
      order       ← Orders.save(Factories.order.copy(customerId = customer.id))
      scReason    ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      scOrigin    ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id,
        reasonId = scReason.id))
      storeCredit ← StoreCredits.save(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      payment ← OrderPayments.save(Factories.storeCreditPayment.copy(orderId = order.id,
        paymentMethodId = storeCredit.id, paymentMethodType = PaymentMethod.StoreCredit))
      storeCreditAdjustments ← StoreCredits.auth(storeCredit, Some(payment.id), 10)
    } yield (admin, customer, scReason, storeCredit, order)).run().futureValue
  }
}

