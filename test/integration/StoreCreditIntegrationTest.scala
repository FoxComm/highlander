import akka.http.scaladsl.model.StatusCodes

import models._
import models.StoreCredit.{Canceled, Active, OnHold}
import responses._
import org.scalatest.BeforeAndAfterEach
import services._
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

          // Check that proper link is created
          val manual = StoreCreditManuals.findOneById(sc.originId).run().futureValue.value
          manual.reasonId must === (scReason.id)
          manual.adminId must === (admin.id)
        }
      }

      "succeeds with valid subTypeId" in new Fixture {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = scReason.id, subTypeId = Some(1))
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)
        val sc = response.as[responses.StoreCreditResponse.Root]

        response.status must === (StatusCodes.OK)
        sc.subTypeId must === (Some(1))
      }

      "fails if subtypeId is not found" in new Fixture {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = scReason.id, subTypeId = Some(255))
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.errors must === (NotFoundFailure(StoreCreditSubtype, 255).description)
      }

      "fails if the customer is not found" in {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = 1)
        val response = POST(s"v1/customers/99/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.errors must === (NotFoundFailure(Customer, 99).description)
      }

      "fails if the reason is not found" in new Fixture {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = 255)
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.errors must === (NotFoundFailure(Reason, 255).description)
      }
    }

    "GET /v1/customers/:id/payment-methods/store-credit" - {
      "returns list of store credits" in new Fixture {
        val response = GET(s"v1/customers/${customer.id}/payment-methods/store-credit")
        val storeCredits = Seq(storeCredit, scSecond)

        response.status must ===(StatusCodes.OK)
        val credits = response.as[Seq[StoreCredit]]
        credits.map(_.id).sorted must ===(storeCredits.map(_.id).sorted)
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
        notFoundResponse.errors mustBe NotFoundFailure(StoreCredit, 99).description
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
        firstAdjustment.orderRef.value mustBe order.referenceNumber
      }
    }

    "PATCH /v1/store-credits/:id" - {
      "successfully changes status from Active to OnHold and vice-versa" in new Fixture {
        val response = PATCH(s"v1/store-credits/${storeCredit.id}", payloads.StoreCreditUpdateStatusByCsr(status = OnHold))
        response.status must ===(StatusCodes.OK)

        val responseBack = PATCH(s"v1/store-credits/${storeCredit.id}", payloads.StoreCreditUpdateStatusByCsr(status = Active))
        responseBack.status must ===(StatusCodes.OK)
      }

      "returns error if no cancellation reason provided" in new Fixture {
        val response = PATCH(s"v1/store-credits/${storeCredit.id}", payloads.StoreCreditUpdateStatusByCsr(status = Canceled))
        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(EmptyCancellationReasonFailure.description)
      }

      "returns error on cancellation if store credit has auths" in new Fixture {
        val response = PATCH(s"v1/store-credits/${storeCredit.id}", payloads.StoreCreditUpdateStatusByCsr(status = Canceled,
          reasonId = Some(1)))
        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(OpenTransactionsFailure.description)
      }

      "successfully cancels store credit with provided reason, cancel adjustment is created" in new Fixture {
        // Cancel pending adjustment
        StoreCreditAdjustments.cancel(adjustment.id).run().futureValue

        val response = PATCH(s"v1/store-credits/${storeCredit.id}", payloads.StoreCreditUpdateStatusByCsr(status = Canceled,
          reasonId = Some(1)))
        response.status must ===(StatusCodes.OK)

        val root = response.as[StoreCreditResponse.Root]
        root.canceledAmount must ===(Some(storeCredit.originalBalance))

        // Ensure that cancel adjustment is automatically created
        val transactionsRep = GET(s"v1/store-credits/${storeCredit.id}/transactions")
        val adjustments = transactionsRep.as[Seq[StoreCreditAdjustmentsResponse.Root]]

        response.status must ===(StatusCodes.OK)
        adjustments.size mustBe 2
        adjustments.head.state must ===(StoreCreditAdjustment.Capture)
      }

      "fails to cancel store credit if invalid reason provided" in new Fixture {
        // Cancel pending adjustment
        StoreCreditAdjustments.cancel(adjustment.id).run().futureValue

        val response = PATCH(s"v1/store-credits/${storeCredit.id}", payloads.StoreCreditUpdateStatusByCsr(status = Canceled,
          reasonId = Some(999)))
        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(InvalidCancellationReasonFailure.description)
      }
    }

    "PATCH /v1/store-credits" - {
      "successfully changes statuses of multiple store credits" in new Fixture {
        val payload = payloads.StoreCreditBulkUpdateStatusByCsr(
          ids = Seq(storeCredit.id, scSecond.id),
          status = StoreCredit.OnHold
        )

        val response = PATCH(s"v1/store-credits", payload)
        response.status must ===(StatusCodes.OK)

        val firstUpdated = StoreCredits.findOneById(storeCredit.id).run().futureValue
        firstUpdated.value.status must ===(StoreCredit.OnHold)

        val secondUpdated = StoreCredits.findOneById(scSecond.id).run().futureValue
        secondUpdated.value.status must ===(StoreCredit.OnHold)
      }

      "returns multiple errors if no cancellation reason provided" in new Fixture {
        val payload = payloads.StoreCreditBulkUpdateStatusByCsr(
          ids = Seq(storeCredit.id, scSecond.id),
          status = StoreCredit.Canceled
        )

        val response = PATCH(s"v1/store-credits", payload)
        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(EmptyCancellationReasonFailure.description)
      }
    }
  }

  trait Fixture {
    val (admin, customer, scReason, storeCredit, order, adjustment, scSecond) = (for {
      admin       ← StoreAdmins.save(authedStoreAdmin)
      customer    ← Customers.save(Factories.customer)
      order       ← Orders.save(Factories.order.copy(customerId = customer.id))
      scReason    ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      scSubType   ← StoreCreditSubtypes.save(Factories.storeCreditSubTypes.head)
      scOrigin    ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id,
        reasonId = scReason.id))
      storeCredit ← StoreCredits.save(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      scSecond ← StoreCredits.save(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer
        .id))
      payment ← OrderPayments.save(Factories.storeCreditPayment.copy(orderId = order.id,
        paymentMethodId = storeCredit.id, paymentMethodType = PaymentMethod.StoreCredit))
      adjustment ← StoreCredits.auth(storeCredit, Some(payment.id), 10)
    } yield (admin, customer, scReason, storeCredit, order, adjustment, scSecond)).run().futureValue
  }
}

