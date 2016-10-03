import akka.http.scaladsl.model.StatusCodes

import Extensions._
import util._
import failures.StoreCreditFailures.StoreCreditConvertFailure
import failures._
import models.customer.{Customer, Customers}
import models.cord.{Carts, OrderPayments}
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit._
import models.payment.storecredit._
import models.payment.{PaymentMethod, giftcard}
import models.{Reason, Reasons, StoreAdmins}
import payloads.PaymentPayloads.CreateManualStoreCredit
import payloads.StoreCreditPayloads._
import responses.{GiftCardResponse, StoreCreditResponse}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class StoreCreditIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with PhoenixAdminApi
    with BakedFixtures {

  "StoreCredits" - {
    "POST /v1/customers/:id/payment-methods/store-credit" - {
      "when successful" - {
        "responds with the new storeCredit" in new Fixture {
          val payload  = CreateManualStoreCredit(amount = 25, reasonId = reason.id)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)
          response.status must === (StatusCodes.OK)

          val sc = response.as[responses.StoreCreditResponse.Root]
          sc.state must === (StoreCredit.Active)

          // Check that proper link is created
          val manual = StoreCreditManuals.findOneById(sc.originId).gimme.value
          manual.reasonId must === (reason.id)
          manual.adminId must === (storeAdmin.id)
        }
      }

      "succeeds with valid subTypeId" in new Fixture {
        val payload =
          CreateManualStoreCredit(amount = 25, reasonId = reason.id, subTypeId = Some(1))
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)
        response.status must === (StatusCodes.OK)

        val sc = response.as[responses.StoreCreditResponse.Root]
        sc.subTypeId must === (Some(1))
      }

      "fails if subtypeId is not found" in new Fixture {
        val payload =
          CreateManualStoreCredit(amount = 25, reasonId = reason.id, subTypeId = Some(255))
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(StoreCreditSubtype, 255).description)
      }

      "fails if the customer is not found" in {
        val payload  = CreateManualStoreCredit(amount = 25, reasonId = 1)
        val response = POST(s"v1/customers/99/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Customer, 99).description)
      }

      "fails if the reason is not found" in new Fixture {
        val payload  = CreateManualStoreCredit(amount = 25, reasonId = 255)
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(Reason, 255).description)
      }
    }

    "GET /v1/customers/:id/payment-methods/store-credit/total" - {
      "returns total available and current store credit for customer" in new Fixture {
        val response = customerAPI.payment.storeCredit.getTotals(customer.id)
        response.status must === (StatusCodes.OK)

        val totals = response.as[StoreCreditResponse.Totals]

        val fst = StoreCredits.refresh(storeCredit).gimme
        val snd = StoreCredits.refresh(scSecond).gimme

        totals.availableBalance must === (fst.availableBalance + snd.availableBalance)
        totals.currentBalance must === (fst.currentBalance + snd.currentBalance)
      }

      "returns 404 when customer doesn't exist" in new Fixture {
        val response = customerAPI.payment.storeCredit.getTotals(99)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Customer, 99).description)
      }
    }

    "PATCH /v1/store-credits/:id" - {
      "successfully changes status from Active to OnHold and vice-versa" in new Fixture {
        val response =
          PATCH(s"v1/store-credits/${storeCredit.id}", StoreCreditUpdateStateByCsr(state = OnHold))
        response.status must === (StatusCodes.OK)

        val responseBack =
          PATCH(s"v1/store-credits/${storeCredit.id}", StoreCreditUpdateStateByCsr(state = Active))
        responseBack.status must === (StatusCodes.OK)
      }

      "returns error if no cancellation reason provided" in new Fixture {
        val response = PATCH(s"v1/store-credits/${storeCredit.id}",
                             StoreCreditUpdateStateByCsr(state = Canceled))
        response.status must === (StatusCodes.BadRequest)
        response.error must === (EmptyCancellationReasonFailure.description)
      }

      "returns error on cancellation if store credit has auths" in new Fixture {
        val response = PATCH(s"v1/store-credits/${storeCredit.id}",
                             StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
        response.status must === (StatusCodes.BadRequest)
        response.error must === (OpenTransactionsFailure.description)
      }

      "successfully cancels store credit with provided reason, cancel adjustment is created" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        StoreCreditAdjustments.cancel(adjustment.id).gimme

        val response = PATCH(s"v1/store-credits/${storeCredit.id}",
                             StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
        response.status must === (StatusCodes.OK)

        val root = response.as[StoreCreditResponse.Root]
        root.canceledAmount must === (Some(storeCredit.originalBalance))

        // Ensure that cancel adjustment is automatically created
        val adjustments = StoreCreditAdjustments.filterByStoreCreditId(storeCredit.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must === (StoreCreditAdjustment.CancellationCapture)
      }

      "successfully cancels store credit with zero balance" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        StoreCreditAdjustments.cancel(adjustment.id).gimme
        // Update balance
        StoreCredits.update(storeCredit, storeCredit.copy(availableBalance = 0)).gimme

        val response = PATCH(s"v1/store-credits/${storeCredit.id}",
                             StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
        response.status must === (StatusCodes.OK)

        val root = response.as[StoreCreditResponse.Root]
        root.canceledAmount must === (Some(0))

        // Ensure that cancel adjustment is automatically created
        val adjustments = StoreCreditAdjustments.filterByStoreCreditId(storeCredit.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must === (StoreCreditAdjustment.CancellationCapture)
      }

      "fails to cancel store credit if invalid reason provided" in new Fixture {
        // Cancel pending adjustment
        StoreCreditAdjustments.cancel(adjustment.id).gimme

        val response = PATCH(s"v1/store-credits/${storeCredit.id}",
                             StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(999)))
        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(Reason, 999).description)
      }
    }

    "PATCH /v1/store-credits" - {
      "successfully changes statuses of multiple store credits" in new Fixture {
        val payload = StoreCreditBulkUpdateStateByCsr(
            ids = Seq(storeCredit.id, scSecond.id),
            state = StoreCredit.OnHold
        )

        val response = PATCH(s"v1/store-credits", payload)
        response.status must === (StatusCodes.OK)

        val firstUpdated = StoreCredits.findOneById(storeCredit.id).gimme
        firstUpdated.value.state must === (StoreCredit.OnHold)

        val secondUpdated = StoreCredits.findOneById(scSecond.id).gimme
        secondUpdated.value.state must === (StoreCredit.OnHold)
      }

      "returns multiple errors if no cancellation reason provided" in new Fixture {
        val payload = StoreCreditBulkUpdateStateByCsr(
            ids = Seq(storeCredit.id, scSecond.id),
            state = StoreCredit.Canceled
        )

        val response = PATCH(s"v1/store-credits", payload)
        response.status must === (StatusCodes.BadRequest)
        response.error must === (EmptyCancellationReasonFailure.description)
      }
    }

    "POST /v1/customers/:customerId/payment-methods/store-credit/:id/convert" - {
      "successfully converts SC to GC" in new Fixture {
        val response =
          POST(s"v1/customers/${customer.id}/payment-methods/store-credit/${scSecond.id}/convert")
        response.status must === (StatusCodes.OK)

        val root = response.as[GiftCardResponse.Root]
        root.originType must === (GiftCard.FromStoreCredit)
        root.state must === (giftcard.GiftCard.Active)
        root.originalBalance must === (scSecond.originalBalance)

        val redeemedSc = StoreCredits.filter(_.id === scSecond.id).one.gimme.value
        redeemedSc.state must === (StoreCredit.FullyRedeemed)
        redeemedSc.availableBalance must === (0)
        redeemedSc.currentBalance must === (0)
      }

      "fails to convert when SC not found" in new Fixture {
        val response =
          POST(s"v1/customers/${customer.id}/payment-methods/store-credit/555/convert")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(StoreCredit, 555).description)
      }

      "fails to convert when customer not found" in new Fixture {
        val response =
          POST(s"v1/customers/666/payment-methods/store-credit/${scSecond.id}/convert")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Customer, 666).description)
      }

      "fails to convert SC to GC if open transactions are present" in new Fixture {
        val response = POST(
            s"v1/customers/${customer.id}/payment-methods/store-credit/${storeCredit.id}/convert")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (OpenTransactionsFailure.description)
      }

      "fails to convert inactive SC to GC" in new Fixture {
        StoreCredits.findActiveById(scSecond.id).map(_.state).update(StoreCredit.OnHold).gimme
        val updatedSc = StoreCredits.findActiveById(scSecond.id).one.gimme.value

        val response =
          POST(s"v1/customers/${customer.id}/payment-methods/store-credit/${scSecond.id}/convert")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (StoreCreditConvertFailure(updatedSc).description)
      }
    }
  }

  trait Fixture extends Reason_Baked with EmptyCustomerCart_Baked {
    val (storeCredit, adjustment, scSecond, payment, scSubType) = (for {
      scSubType ← * <~ StoreCreditSubtypes.create(Factories.storeCreditSubTypes.head)
      scOrigin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.id, reasonId = reason.id))
      storeCredit ← * <~ StoreCredits.create(
                       Factories.storeCredit.copy(originId = scOrigin.id,
                                                  customerId = customer.id))
      scSecond ← * <~ StoreCredits.create(
                    Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      payment ← * <~ OrderPayments.create(
                   Factories.storeCreditPayment.copy(cordRef = cart.refNum,
                                                     paymentMethodId = storeCredit.id,
                                                     paymentMethodType = PaymentMethod.StoreCredit,
                                                     amount = Some(storeCredit.availableBalance)))
      adjustment ← * <~ StoreCredits.auth(storeCredit, Some(payment.id), 10)
    } yield (storeCredit, adjustment, scSecond, payment, scSubType)).gimme
  }
}
