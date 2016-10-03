import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import cats.implicits._
import failures.GiftCardFailures.GiftCardConvertFailure
import failures._
import models.Reason
import models.customer.Customer
import models.payment.giftcard.GiftCard._
import models.payment.giftcard._
import models.payment.storecredit
import models.payment.storecredit.StoreCredit
import payloads.GiftCardPayloads._
import responses.GiftCardResponse.Root
import responses.{GiftCardAdjustmentsResponse, _}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import util.apis.PhoenixAdminApi
import util.fixtures.BakedFixtures
import utils.Money._
import utils.db._

class GiftCardIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "GiftCards" - {

    "POST /v1/gift-cards" - {
      "successfully creates gift card from payload" in new Reason_Baked {
        val payload = GiftCardCreateByCsr(balance = 555, reasonId = reason.id)
        val root    = giftCardsApi.create(payload).as[Root]

        root.originType must === (GiftCard.CsrAppeasement)
        root.currency must === (Currency.USD)
        root.availableBalance must === (555)

        // Check that proper link is created
        val manual = GiftCardManuals.findOneById(root.originId).gimme.value
        manual.reasonId must === (1)
        manual.adminId must === (storeAdmin.id)
      }

      "create two gift cards with unique codes" in new Reason_Baked {
        val payload = GiftCardCreateByCsr(balance = 555, reasonId = reason.id)

        val rootFirst  = giftCardsApi.create(payload).as[Root]
        val rootSecond = giftCardsApi.create(payload).as[Root]
        rootFirst.code must !==(rootSecond.code)
      }

      "succeeds with valid subTypeId" in new Reason_Baked with GiftCardSubtype_Seed {
        val payload = GiftCardCreateByCsr(balance = 25,
                                          reasonId = reason.id,
                                          subTypeId = giftCardSubtype.id.some)
        val sc = giftCardsApi.create(payload).as[Root]
        sc.subTypeId.value must === (1)
      }

      "fails if subtypeId is not found" in new Reason_Baked {
        val payload = GiftCardCreateByCsr(balance = 25, reasonId = reason.id, subTypeId = 255.some)
        giftCardsApi.create(payload).mustFailWith400(NotFoundFailure404(GiftCardSubtype, 255))
      }

      "fails to create gift card with negative balance" in new Reason_Baked {
        giftCardsApi
          .create(GiftCardCreateByCsr(balance = -555, reasonId = reason.id))
          .mustFailWithMessage("Balance got -555, expected more than 0")
      }

      "fails to create gift card with invalid reason" in new Reason_Baked {
        giftCardsApi
          .create(GiftCardCreateByCsr(balance = 555, reasonId = 999))
          .mustFailWith400(NotFoundFailure404(Reason, 999))
      }
    }

    "POST /v1/gift-cards (bulk)" - {
      "successfully creates multiple gift cards from payload" in new Reason_Baked {
        val root = giftCardsApi
          .createBulk(GiftCardBulkCreateByCsr(quantity = 5, balance = 256, reasonId = reason.id))
          .as[Seq[GiftCardBulkResponse.ItemResult]]
        root.length must === (5)
      }

      "fails to create multiple gift cards with zero balance" in new Reason_Baked {
        giftCardsApi
          .createBulk(GiftCardBulkCreateByCsr(quantity = 5, balance = 0, reasonId = reason.id))
          .mustFailWithMessage("Balance got 0, expected more than 0")
      }

      "fails to create multiple gift cards with negative balance" in new Reason_Baked {
        giftCardsApi
          .createBulk(GiftCardBulkCreateByCsr(quantity = 5, balance = -555, reasonId = reason.id))
          .mustFailWithMessage("Balance got -555, expected more than 0")
      }

      "fails to create multiple gift cards with negative quantity" in new Reason_Baked {
        giftCardsApi
          .createBulk(GiftCardBulkCreateByCsr(quantity = -5, balance = 256, reasonId = reason.id))
          .mustFailWithMessage("Quantity got -5, expected more than 0")
      }

      "fails to create multiple gift cards with count more than limit" in new Reason_Baked {
        giftCardsApi
          .createBulk(GiftCardBulkCreateByCsr(quantity = 25, balance = 256, reasonId = reason.id))
          .mustFailWithMessage("Quantity got 25, expected 20 or less")
      }
    }

    "GET /v1/gift-cards/:code" - {
      "finds a gift card by code" in new GiftCard_Baked {
        giftCardsApi(giftCard.code).get().as[Root].code must === (giftCard.code)
      }

      "returns not found when GC doesn't exist" in {
        giftCardsApi("ABC-666").get().mustFailWith404(NotFoundFailure404(GiftCard, "ABC-666"))
      }
    }

    "PATCH /v1/gift-cards/:code" - {
      "successfully changes state from Active to OnHold and vice-versa" in new GiftCard_Baked {
        giftCardsApi(giftCard.code).update(GiftCardUpdateStateByCsr(state = OnHold)).mustBeOk()
        giftCardsApi(giftCard.code).update(GiftCardUpdateStateByCsr(state = Active)).mustBeOk()
      }

      "returns error if no cancellation reason provided" in new GiftCard_Baked {
        giftCardsApi(giftCard.code)
          .update(GiftCardUpdateStateByCsr(state = Canceled))
          .mustFailWith400(EmptyCancellationReasonFailure)
      }

      "returns error on cancellation if gift card has auths" in new Fixture {
        giftCardsApi(giftCard1.code)
          .update(GiftCardUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .mustFailWith400(OpenTransactionsFailure)
      }

      "successfully cancels gift card with provided reason, cancel adjustment is created" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        GiftCardAdjustments.cancel(adjustment1.id).gimme

        val root = giftCardsApi(giftCard1.code)
          .update(GiftCardUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .as[Root]
        root.canceledAmount must === (Some(giftCard1.originalBalance))

        // Ensure that cancel adjustment is automatically created
        val adjustments = GiftCardAdjustments.filterByGiftCardId(giftCard1.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must === (GiftCardAdjustment.CancellationCapture)
      }

      "successfully cancels gift card with zero balance" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        GiftCardAdjustments.cancel(adjustment1.id).gimme
        // Update balance
        GiftCards.update(giftCard1, giftCard1.copy(availableBalance = 0)).gimme

        val root = giftCardsApi(giftCard1.code)
          .update(GiftCardUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .as[Root]
        root.canceledAmount.value must === (0)

        // Ensure that cancel adjustment is automatically created
        val adjustments = GiftCardAdjustments.filterByGiftCardId(giftCard1.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must === (GiftCardAdjustment.CancellationCapture)
      }

      "fails to cancel gift card if invalid reason provided" in new Fixture {
        GiftCardAdjustments.cancel(adjustment1.id).gimme

        giftCardsApi(giftCard1.code)
          .update(GiftCardUpdateStateByCsr(state = Canceled, reasonId = 999.some))
          .mustFailWith400(NotFoundFailure400(Reason, 999))
      }
    }

    "GET /v1/gift-cards/:code/transactions" - {
      "returns the list of adjustments" in new Fixture {
        val adjustments =
          giftCardsApi(giftCard1.code).transactions().as[Seq[GiftCardAdjustmentsResponse.Root]]

        adjustments.size mustBe 1

        val firstAdjustment = adjustments.head
        firstAdjustment.amount must === (-adjustment1.debit)
        firstAdjustment.availableBalance must === (giftCard1.originalBalance - adjustment1.debit)
        firstAdjustment.cordRef.value mustBe cart.referenceNumber
      }
    }

    "PATCH /v1/gift-cards" - {
      "successfully changes statuses of multiple gift cards" in new Fixture {
        val payload = GiftCardBulkUpdateStateByCsr(
            codes = Seq(giftCard1.code, giftCard2.code),
            state = GiftCard.OnHold
        )

        giftCardsApi.updateBulk(payload).mustBeOk()

        val firstUpdated = GiftCards.findOneById(giftCard1.id).gimme
        firstUpdated.value.state must === (GiftCard.OnHold)

        val secondUpdated = GiftCards.findOneById(giftCard2.id).gimme
        secondUpdated.value.state must === (GiftCard.OnHold)
      }

      "returns multiple errors if no cancellation reason provided" in new Fixture {
        val payload = GiftCardBulkUpdateStateByCsr(
            codes = Seq(giftCard1.code, giftCard2.code),
            state = GiftCard.Canceled
        )

        giftCardsApi.updateBulk(payload).mustFailWith400(EmptyCancellationReasonFailure)
      }
    }

    "POST /v1/gift-cards/:code/convert/:customerId" - {
      "successfully converts GC to SC" in new Fixture {
        val root = giftCardsApi(giftCard2.code)
          .convertToStoreCredit(customer.id)
          .as[StoreCreditResponse.Root]

        root.customerId must === (customer.id)
        root.originType must === (StoreCredit.GiftCardTransfer)
        root.state must === (storecredit.StoreCredit.Active)
        root.originalBalance must === (giftCard2.originalBalance)

        val redeemedGc = GiftCards.findByCode(giftCard2.code).one.gimme.value
        redeemedGc.state must === (GiftCard.FullyRedeemed)
        redeemedGc.availableBalance must === (0)
        redeemedGc.currentBalance must === (0)
      }

      "fails to convert when GC not found" in new Fixture {
        giftCardsApi("ABC-666")
          .convertToStoreCredit(customer.id)
          .mustFailWith404(NotFoundFailure404(GiftCard, "ABC-666"))
      }

      "fails to convert when customer not found" in new Fixture {
        giftCardsApi(giftCard2.code)
          .convertToStoreCredit(666)
          .mustFailWith404(NotFoundFailure404(Customer, 666))
      }

      "fails to convert GC to SC if open transactions are present" in new Fixture {
        giftCardsApi(giftCard1.code)
          .convertToStoreCredit(customer.id)
          .mustFailWith400(OpenTransactionsFailure)
      }

      "fails to convert inactive GC to SC" in new Fixture {
        GiftCards.findByCode(giftCard2.code).map(_.state).update(GiftCard.OnHold).gimme
        val updatedGc = GiftCards.findByCode(giftCard2.code).one.gimme

        giftCardsApi(giftCard2.code)
          .convertToStoreCredit(customer.id)
          .mustFailWith400(GiftCardConvertFailure(updatedGc.value))
      }
    }
  }

  trait GiftCard_Baked extends Reason_Baked with GiftCard_Raw {
    override def giftCardBalance = 1500
  }

  trait Fixture extends GiftCardSubtype_Seed with Reason_Baked {
    val giftCard1 = new GiftCard_Baked {}.giftCard
    val giftCard2 = new GiftCard_Baked {}.giftCard
    object setup
        extends StoreAdmin_Seed
        with Customer_Seed
        with EmptyCart_Raw
        with CartWithGiftCardPayment_Raw {
      override def giftCard        = giftCard1
      override def gcPaymentAmount = 25
    }

    val (cart, customer, payment) = (setup.cart, setup.customer, setup.orderPayments.head)

    // TODO: replace with checkout?
    val adjustment1 = GiftCards.auth(giftCard1, Some(payment.id), 10).gimme
  }
}
