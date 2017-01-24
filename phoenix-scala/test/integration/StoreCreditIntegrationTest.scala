import cats.implicits._
import failures.StoreCreditFailures.StoreCreditConvertFailure
import failures._
import models.Reason
import models.account._
import models.cord.OrderPayments
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit._
import models.payment.storecredit._
import models.payment.{InStorePaymentStates, giftcard}
import payloads.PaymentPayloads.{CreateManualStoreCredit, StoreCreditPayment}
import payloads.StoreCreditPayloads._
import responses.StoreCreditResponse.Root
import responses.{GiftCardResponse, StoreCreditResponse}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import utils.db._

class StoreCreditIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with PhoenixAdminApi
    with ApiFixtures
    with ApiFixtureHelpers
    with BakedFixtures {

  "StoreCredits" - {
    "POST /v1/customers/:id/payment-methods/store-credit" - {
      "when successful" - {
        "responds with the new storeCredit" in new Fixture {
          val payload = CreateManualStoreCredit(amount = 25, reasonId = reason.id)
          customersApi(customerId).payments.storeCredit.create(payload).as[Root].state must === (
              StoreCredit.Active)

          // Check that proper link is created
          val originId =
            customersApi(customerId).payments.storeCredit.create(payload).as[Root].originId
          val manual = StoreCreditManuals.findOneById(originId).gimme.value
          manual.reasonId must === (reason.id)
          manual.adminId must === (storeAdmin.accountId)
        }
      }

      "succeeds with valid subTypeId" in new Fixture {
        customersApi(customerId).payments.storeCredit
          .create(CreateManualStoreCredit(amount = 25,
                                          reasonId = reason.id,
                                          subTypeId = Some(subtype.id)))
          .as[Root]
          .subTypeId
          .value must === (1)
      }

      "fails if subtypeId is not found" in new Fixture {
        customersApi(customerId).payments.storeCredit
          .create(CreateManualStoreCredit(amount = 25, reasonId = reason.id, subTypeId = 255.some))
          .mustFailWith400(NotFoundFailure404(StoreCreditSubtype, 255))
      }

      "fails if the customer is not found" in new Reason_Baked {
        customersApi(99).payments.storeCredit
          .create(CreateManualStoreCredit(amount = 25, reasonId = reason.id))
          .mustFailWith404(NotFoundFailure404(User, 99))
      }

      "fails if the reason is not found" in new Fixture {
        customersApi(customerId).payments.storeCredit
          .create(CreateManualStoreCredit(amount = 25, reasonId = 255))
          .mustFailWith400(NotFoundFailure404(Reason, 255))
      }
    }

    "GET /v1/customers/:id/payment-methods/store-credit/total" - {
      "returns total available and current store credit for customer" in new Fixture {
        val totals =
          customersApi(customerId).payments.storeCredit.totals().as[StoreCreditResponse.Totals]

        val bothStoreCredits = Seq(adjustedStoreCredit, storeCredit)

        totals.availableBalance must === (
            bothStoreCredits.map(_.availableBalance).sum - adjustmentAmount)
        totals.currentBalance must === (bothStoreCredits.map(_.currentBalance).sum)
      }

      "returns 404 when customer doesn't exist" in new Fixture {
        customersApi(99).payments.storeCredit
          .totals()
          .mustFailWith404(NotFoundFailure404(User, 99))
      }
    }

    "PATCH /v1/store-credits/:id" - {
      "successfully changes status from Active to OnHold and vice-versa" in new Fixture {
        storeCreditsApi(adjustedStoreCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = OnHold))
          .mustBeOk()
        storeCreditsApi(adjustedStoreCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Active))
          .mustBeOk()
      }

      "returns error if no cancellation reason provided" in new Fixture {
        storeCreditsApi(adjustedStoreCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled))
          .mustFailWith400(EmptyCancellationReasonFailure)
      }

      "returns error on cancellation if store credit has auths" in new Fixture {
        storeCreditsApi(adjustedStoreCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .mustFailWith400(OpenTransactionsFailure)
      }

      "successfully cancels store credit with provided reason, cancel adjustment is created" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        StoreCreditAdjustments.cancel(adjustment.id).gimme

        storeCreditsApi(adjustedStoreCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .as[Root]
          .canceledAmount
          .value must === (adjustedStoreCredit.originalBalance)

        // Ensure that cancel adjustment is automatically created
        val adjustments =
          StoreCreditAdjustments.filterByStoreCreditId(adjustedStoreCredit.id).gimme
        adjustments must have size 2
        adjustments.head.state must === (InStorePaymentStates.CancellationCapture)
      }

      "successfully cancels store credit with zero balance" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        StoreCreditAdjustments.cancel(adjustment.id).gimme

        StoreCredits.update(adjustedScModel, adjustedScModel.copy(availableBalance = 0)).gimme

        storeCreditsApi(adjustedStoreCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .as[Root]
          .canceledAmount
          .value must === (0)

        // Ensure that cancel adjustment is automatically created
        val adjustments =
          StoreCreditAdjustments.filterByStoreCreditId(adjustedStoreCredit.id).gimme
        adjustments must have size 2
        adjustments.head.state must === (InStorePaymentStates.CancellationCapture)
      }

      "fails to cancel store credit if invalid reason provided" in new Fixture {
        StoreCreditAdjustments.cancel(adjustment.id).gimme

        val response = storeCreditsApi(adjustedStoreCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = 999.some))
          .mustFailWith400(NotFoundFailure400(Reason, 999))
      }
    }

    "PATCH /v1/store-credits" - {
      "successfully changes statuses of multiple store credits" in new Fixture {
        val payload = StoreCreditBulkUpdateStateByCsr(
            ids = Seq(adjustedStoreCredit.id, storeCredit.id),
            state = StoreCredit.OnHold
        )

        storeCreditsApi.update(payload).mustBeOk()

        val firstUpdated = StoreCredits.findOneById(adjustedStoreCredit.id).gimme
        firstUpdated.value.state must === (StoreCredit.OnHold)

        val secondUpdated = StoreCredits.findOneById(storeCredit.id).gimme
        secondUpdated.value.state must === (StoreCredit.OnHold)
      }

      "returns multiple errors if no cancellation reason provided" in new Fixture {
        val payload = StoreCreditBulkUpdateStateByCsr(
            ids = Seq(adjustedStoreCredit.id, storeCredit.id),
            state = StoreCredit.Canceled
        )

        storeCreditsApi.update(payload).mustFailWith400(EmptyCancellationReasonFailure)
      }
    }

    "POST /v1/customers/:customerId/payment-methods/store-credit/:id/convert" - {
      "successfully converts SC to GC" in new Fixture {
        val gcFromSc = customersApi(customerId).payments
          .storeCredit(storeCredit.id)
          .convert()
          .as[GiftCardResponse.Root]

        gcFromSc.originType must === (GiftCard.FromStoreCredit)
        gcFromSc.state must === (giftcard.GiftCard.Active)
        gcFromSc.originalBalance must === (storeCredit.originalBalance)

        val redeemedSc = StoreCredits.mustFindById400(storeCredit.id).gimme
        redeemedSc.state must === (StoreCredit.FullyRedeemed)
        redeemedSc.availableBalance must === (0)
        redeemedSc.currentBalance must === (0)
      }

      "fails to convert when SC not found" in new Fixture {
        customersApi(customerId).payments
          .storeCredit(555)
          .convert()
          .mustFailWith404(NotFoundFailure404(StoreCredit, 555))
      }

      "fails to convert when customer not found" in new Fixture {
        customersApi(666).payments
          .storeCredit(storeCredit.id)
          .convert()
          .mustFailWith404(NotFoundFailure404(User, 666))
      }

      "fails to convert SC to GC if open transactions are present" in new Fixture {
        customersApi(customerId).payments
          .storeCredit(adjustedStoreCredit.id)
          .convert()
          .mustFailWith400(OpenTransactionsFailure)
      }

      "fails to convert inactive SC to GC" in new Fixture {
        storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(StoreCredit.OnHold))
          .mustBeOk()

        customersApi(customerId).payments
          .storeCredit(storeCredit.id)
          .convert()
          .mustFailWith400(StoreCreditConvertFailure(StoreCredit.OnHold))
      }
    }
  }

  trait Fixture extends Reason_Baked {

    val customerId = api_newCustomer().id
    val cartRef    = api_newCustomerCart(customerId).referenceNumber

    val adjustedStoreCredit = customersApi(customerId).payments.storeCredit
      .create(CreateManualStoreCredit(amount = 5000, reasonId = reason.id))
      .as[StoreCreditResponse.Root]

    val storeCredit = customersApi(customerId).payments.storeCredit
      .create(CreateManualStoreCredit(amount = 2000, reasonId = reason.id))
      .as[StoreCreditResponse.Root]

    cartsApi(cartRef).payments.storeCredit
      .add(StoreCreditPayment(amount = adjustedStoreCredit.availableBalance))
      .mustBeOk()

    val adjustmentAmount = 10

    val (payment, adjustedScModel, adjustment, subtype) = (for {
      gcSubtype ← * <~ StoreCreditSubtypes.create(
                     StoreCreditSubtype(title = "foo", originType = CsrAppeasement))
      storeCreditModel ← * <~ StoreCredits.mustFindById400(adjustedStoreCredit.id)
      payment          ← * <~ OrderPayments.findAllByCordRef(cartRef).one
      // FIXME @anna Must be replaced by checkout
      adjustment ← * <~ StoreCredits.auth(storeCreditModel,
                                          payment.value.id.some,
                                          adjustmentAmount)
    } yield (payment.value, storeCreditModel, adjustment, gcSubtype)).gimme
  }
}
