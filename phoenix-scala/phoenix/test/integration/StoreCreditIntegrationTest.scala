import core.failures._
import phoenix.failures.StoreCreditFailures.StoreCreditConvertFailure
import phoenix.failures.{EmptyCancellationReasonFailure, OpenTransactionsFailure}
import phoenix.models.Reason
import phoenix.models.account._
import phoenix.models.cord.OrderPayments
import phoenix.models.payment.giftcard.GiftCard
import phoenix.models.payment.storecredit.StoreCredit._
import phoenix.models.payment.storecredit._
import phoenix.models.payment.{InStorePaymentStates, PaymentMethod, giftcard}
import phoenix.payloads.PaymentPayloads.CreateManualStoreCredit
import phoenix.payloads.StoreCreditPayloads._
import phoenix.responses.StoreCreditResponse.Root
import phoenix.responses.{GiftCardResponse, StoreCreditResponse}
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.db._

class StoreCreditIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with DefaultJwtAdminAuth
    with PhoenixAdminApi
    with BakedFixtures {

  "StoreCredits" - {
    "POST /v1/customers/:id/payment-methods/store-credit" - {
      "when successful" - {
        "responds with the new storeCredit" in new Fixture {
          val payload = CreateManualStoreCredit(amount = 25, reasonId = reason.id)
          val sc      = customersApi(customer.accountId).payments.storeCredit.create(payload).as[Root]
          sc.state must === (StoreCredit.Active)

          // Check that proper link is created
          val manual = StoreCreditManuals.findOneById(sc.originId).gimme.value
          manual.reasonId must === (reason.id)
          manual.adminId must === (defaultAdmin.id)
        }
      }

      "succeeds with valid subTypeId" in new Fixture {
        customersApi(customer.accountId).payments.storeCredit
          .create(CreateManualStoreCredit(amount = 25, reasonId = reason.id, subTypeId = Some(1)))
          .as[Root]
          .subTypeId must === (Some(1))
      }

      "fails if subtypeId is not found" in new Fixture {
        customersApi(customer.accountId).payments.storeCredit
          .create(
              CreateManualStoreCredit(amount = 25, reasonId = reason.id, subTypeId = Some(255)))
          .mustFailWith400(NotFoundFailure404(StoreCreditSubtype, 255))
      }

      "fails if the customer is not found" in {
        customersApi(99).payments.storeCredit
          .create(CreateManualStoreCredit(amount = 25, reasonId = 1))
          .mustFailWith404(NotFoundFailure404(User, 99))
      }

      "fails if the reason is not found" in new Fixture {
        customersApi(customer.accountId).payments.storeCredit
          .create(CreateManualStoreCredit(amount = 25, reasonId = 255))
          .mustFailWith400(NotFoundFailure404(Reason, 255))
      }
    }

    "GET /v1/customers/:id/payment-methods/store-credit/total" - {
      "returns total available and current store credit for customer" in new Fixture {
        val totals = customersApi(customer.accountId).payments.storeCredit
          .totals()
          .as[StoreCreditResponse.Totals]

        val fst = StoreCredits.refresh(storeCredit).gimme
        val snd = StoreCredits.refresh(scSecond).gimme

        totals.availableBalance must === (fst.availableBalance + snd.availableBalance)
        totals.currentBalance must === (fst.currentBalance + snd.currentBalance)
      }

      "returns 404 when customer doesn't exist" in new Fixture {
        customersApi(99).payments.storeCredit
          .totals()
          .mustFailWith404(NotFoundFailure404(User, 99))
      }
    }

    "PATCH /v1/store-credits/:id" - {
      "successfully changes status from Active to OnHold and vice-versa" in new Fixture {
        storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = OnHold))
          .mustBeOk()
        storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Active))
          .mustBeOk()
      }

      "returns error if no cancellation reason provided" in new Fixture {
        storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled))
          .mustFailWith400(EmptyCancellationReasonFailure)
      }

      "returns error on cancellation if store credit has auths" in new Fixture {
        storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
          .mustFailWith400(OpenTransactionsFailure)
      }

      "successfully cancels store credit with provided reason, cancel adjustment is created" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        StoreCreditAdjustments.cancel(adjustment.id).gimme

        val root = storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
          .as[Root]
        root.canceledAmount must === (Some(storeCredit.originalBalance))

        // Ensure that cancel adjustment is automatically created
        val adjustments = StoreCreditAdjustments.filterByStoreCreditId(storeCredit.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must === (InStorePaymentStates.CancellationCapture)
      }

      "successfully cancels store credit with zero balance" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        StoreCreditAdjustments.cancel(adjustment.id).gimme
        // Update balance
        StoreCredits.update(storeCredit, storeCredit.copy(availableBalance = 0)).gimme

        val root = storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
          .as[Root]
        root.canceledAmount must === (Some(0))

        // Ensure that cancel adjustment is automatically created
        val adjustments = StoreCreditAdjustments.filterByStoreCreditId(storeCredit.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must === (InStorePaymentStates.CancellationCapture)
      }

      "fails to cancel store credit if invalid reason provided" in new Fixture {
        StoreCreditAdjustments.cancel(adjustment.id).gimme

        val response = storeCreditsApi(storeCredit.id)
          .update(StoreCreditUpdateStateByCsr(state = Canceled, reasonId = Some(999)))
          .mustFailWith400(NotFoundFailure400(Reason, 999))
      }
    }

    "PATCH /v1/store-credits" - {
      "successfully changes statuses of multiple store credits" in new Fixture {
        val payload = StoreCreditBulkUpdateStateByCsr(
            ids = Seq(storeCredit.id, scSecond.id),
            state = StoreCredit.OnHold
        )

        storeCreditsApi.update(payload).mustBeOk()

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

        storeCreditsApi.update(payload).mustFailWith400(EmptyCancellationReasonFailure)
      }
    }

    "POST /v1/customers/:customerId/payment-methods/store-credit/:id/convert" - {
      "successfully converts SC to GC" in new Fixture {
        val root = customersApi(customer.accountId).payments
          .storeCredit(scSecond.id)
          .convert()
          .as[GiftCardResponse.Root]

        root.originType must === (GiftCard.FromStoreCredit)
        root.state must === (giftcard.GiftCard.Active)
        root.originalBalance must === (scSecond.originalBalance)

        val redeemedSc = StoreCredits.filter(_.id === scSecond.id).one.gimme.value
        redeemedSc.state must === (StoreCredit.FullyRedeemed)
        redeemedSc.availableBalance must === (0)
        redeemedSc.currentBalance must === (0)
      }

      "fails to convert when SC not found" in new Fixture {
        customersApi(customer.accountId).payments
          .storeCredit(555)
          .convert()
          .mustFailWith404(NotFoundFailure404(StoreCredit, 555))
      }

      "fails to convert when customer not found" in new Fixture {
        customersApi(666).payments
          .storeCredit(scSecond.id)
          .convert()
          .mustFailWith404(NotFoundFailure404(User, 666))
      }

      "fails to convert SC to GC if open transactions are present" in new Fixture {
        customersApi(customer.accountId).payments
          .storeCredit(storeCredit.id)
          .convert()
          .mustFailWith400(OpenTransactionsFailure)
      }

      "fails to convert inactive SC to GC" in new Fixture {
        StoreCredits.findActiveById(scSecond.id).map(_.state).update(StoreCredit.OnHold).gimme
        val updatedSc = StoreCredits.findActiveById(scSecond.id).one.gimme.value

        customersApi(customer.accountId).payments
          .storeCredit(scSecond.id)
          .convert()
          .mustFailWith400(StoreCreditConvertFailure(updatedSc))
      }
    }
  }

  trait Fixture extends Reason_Baked with EmptyCustomerCart_Baked {
    val (storeCredit, adjustment, scSecond, payment, scSubType) = (for {
      scSubType ← * <~ StoreCreditSubtypes.create(Factories.storeCreditSubTypes.head)
      scOrigin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      storeCredit ← * <~ StoreCredits.create(
                       Factories.storeCredit.copy(originId = scOrigin.id,
                                                  accountId = customer.accountId))
      scSecond ← * <~ StoreCredits.create(
                    Factories.storeCredit.copy(originId = scOrigin.id,
                                               accountId = customer.accountId))
      payment ← * <~ OrderPayments.create(
                   Factories.storeCreditPayment.copy(cordRef = cart.refNum,
                                                     paymentMethodId = storeCredit.id,
                                                     paymentMethodType = PaymentMethod.StoreCredit,
                                                     amount = Some(storeCredit.availableBalance)))
      adjustment ← * <~ StoreCredits.auth(storeCredit, payment.id, 10)
    } yield (storeCredit, adjustment, scSecond, payment, scSubType)).gimme
  }
}
