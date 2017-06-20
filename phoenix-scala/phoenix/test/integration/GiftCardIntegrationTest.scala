import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.db._
import core.failures._
import core.utils.Money._
import phoenix.failures.GiftCardFailures.GiftCardConvertFailure
import phoenix.failures.ScopeFailures._
import phoenix.failures.{EmptyCancellationReasonFailure, OpenTransactionsFailure}
import phoenix.models.Reason
import phoenix.models.account._
import phoenix.models.payment.giftcard.GiftCard._
import phoenix.models.payment.giftcard._
import phoenix.models.payment.storecredit.StoreCredit
import phoenix.models.payment.{storecredit, InStorePaymentStates}
import phoenix.payloads.GiftCardPayloads._
import phoenix.responses.StoreCreditResponse.{Root ⇒ ScRoot}
import phoenix.responses.giftcards._
import slick.jdbc.PostgresProfile.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtureHelpers

class GiftCardIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures
    with ApiFixtureHelpers {

  "GiftCards" - {

    "POST /v1/gift-cards" - {
      "successfully creates gift card from payload" in new Reason_Baked {
        private val payload            = GiftCardCreateByCsr(balance = 555, reasonId = reason.id)
        val giftCard: GiftCardResponse = giftCardsApi.create(payload).as[GiftCardResponse]

        giftCard.originType must === (GiftCard.CsrAppeasement)
        giftCard.currency must === (Currency.USD)
        giftCard.availableBalance must === (555)

        // Check that proper link is created
        val manual: GiftCardManual = GiftCardManuals.findOneById(giftCard.originId).gimme.value
        manual.reasonId must === (reason.id)
        manual.adminId must === (defaultAdmin.id)
      }

      "create two gift cards with unique codes" in new Reason_Baked {
        private val payload = GiftCardCreateByCsr(balance = 555, reasonId = reason.id)

        private val code1 = giftCardsApi.create(payload).as[GiftCardResponse].code
        private val code2 = giftCardsApi.create(payload).as[GiftCardResponse].code
        code1 must !==(code2)
      }

      "succeeds with valid subTypeId" in new Reason_Baked with GiftCardSubtype_Seed {
        private val payload =
          GiftCardCreateByCsr(balance = 25, reasonId = reason.id, subTypeId = giftCardSubtype.id.some)

        giftCardsApi.create(payload).as[GiftCardResponse].subTypeId.value must === (giftCardSubtype.id)
      }

      "fails if subtypeId is not found" in new Reason_Baked {
        giftCardsApi
          .create(GiftCardCreateByCsr(balance = 25, reasonId = reason.id, subTypeId = 255.some))
          .mustFailWith400(NotFoundFailure404(GiftCardSubtype, 255))
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

      "overrides scope" in new Reason_Baked {
        val gc1Code =
          giftCardsApi.create(GiftCardCreateByCsr(balance = 100, reason.id)).as[GiftCardResponse].code

        GiftCards.mustFindByCode(gc1Code).gimme.scope must === (LTree("1"))

        val gc2Code = giftCardsApi
          .create(GiftCardCreateByCsr(balance = 100, reasonId = reason.id, scope = "1.2".some))
          .as[GiftCardResponse]
          .code

        GiftCards.mustFindByCode(gc2Code).gimme.scope must === (LTree("1.2"))
      }

      "refuses to override empty scope" in new Reason_Baked {
        giftCardsApi
          .create(GiftCardCreateByCsr(balance = 100, reasonId = reason.id, scope = "".some))
          .mustFailWithMessage("scope must not be empty")
      }

      "refuses to override invalid scope" in new Reason_Baked {
        giftCardsApi
          .create(GiftCardCreateByCsr(balance = 100, reasonId = reason.id, scope = "2".some))
          .mustFailWith400(InvalidSubscope("1", "2"))

        giftCardsApi
          .create(GiftCardCreateByCsr(balance = 100, reasonId = reason.id, scope = "2.2".some))
          .mustFailWith400(InvalidSubscope("1", "2.2"))
      }
    }

    "POST /v1/customer-gift-cards" - {
      "successfully creates gift card as a customer from payload" in new Fixture {
        val cordInsert = api_newCustomerCart(customer.accountId)

        val root = giftCardsApi
          .createFromCustomer(GiftCardCreatedByCustomer(
            balance = 555,
            senderName = "senderName",
            recipientName = "recipienName",
            recipientEmail = "recipientEmail@mail.com",
            message = "test message".some,
            cordRef = cordInsert.referenceNumber
          ))
          .as[GiftCardResponse]
        root.currency must === (Currency.USD)
        root.availableBalance must === (555)
        root.message.get must === ("test message")
        root.senderName.get must === ("senderName")
        root.recipientEmail.get must === ("recipientEmail@mail.com")
      }

      "successfully creates gift cards as a customer from payload" in new Fixture {
        val cordInsert = api_newCustomerCart(customer.accountId)

        val root = giftCardsApi
          .createMultipleFromCustomer(Seq(
            GiftCardCreatedByCustomer(
              balance = 555,
              senderName = "senderName",
              recipientName = "recipienName",
              recipientEmail = "recipientEmail@mail.com",
              message = "test message".some,
              cordRef = cordInsert.referenceNumber
            ),
            GiftCardCreatedByCustomer(
              balance = 100,
              senderName = "senderName2",
              recipientName = "recipienName2",
              recipientEmail = "recipientEmail@mail.com2",
              message = "test message2".some,
              cordRef = cordInsert.referenceNumber
            )
          ))
          .as[Seq[GiftCardResponse]]

        root.size must === (2)

        root.head.currency must === (Currency.USD)
        root.head.availableBalance must === (555)
        root.tail.head.currency must === (Currency.USD)
        root.tail.head.availableBalance must === (100)
        root.head.message.get must === ("test message")
        root.head.senderName.get must === ("senderName")
        root.tail.head.recipientEmail.get must === ("recipientEmail@mail.com2")
      }

      "successfully creates gift cards with empty messages as a customer from payload" in new Fixture {
        val cordInsert = api_newCustomerCart(customer.accountId)

        val root = giftCardsApi
          .createMultipleFromCustomer(Seq(
            GiftCardCreatedByCustomer(
              balance = 555,
              senderName = "senderName",
              recipientName = "recipienName",
              recipientEmail = "recipientEmail@mail.com",
              message = None,
              cordRef = cordInsert.referenceNumber
            ),
            GiftCardCreatedByCustomer(
              balance = 100,
              senderName = "senderName2",
              recipientName = "recipienName2",
              recipientEmail = "recipientEmail@mail.com2",
              message = "".some,
              cordRef = cordInsert.referenceNumber
            )
          ))
          .as[Seq[GiftCardResponse]]

        root.size must === (2)

        root.map { gc ⇒
          (gc.currency, gc.availableBalance, gc.message)
        } must contain theSameElementsAs Seq((Currency.USD, 555, None), (Currency.USD, 100, None))
      }
    }

    "POST /v1/gift-cards (bulk)" - {
      "successfully creates multiple gift cards from payload" in new Reason_Baked {
        giftCardsApi
          .createBulk(GiftCardBulkCreateByCsr(quantity = 5, balance = 256, reasonId = reason.id))
          .as[Seq[GiftCardBulkResponse]] must have size 5
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
        giftCardsApi(giftCard.code).get().as[GiftCardResponse].code must === (giftCard.code)
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

        giftCardsApi(giftCard1.code)
          .update(GiftCardUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .as[GiftCardResponse]
          .canceledAmount must === (Some(giftCard1.originalBalance))

        // Ensure that cancel adjustment is automatically created
        val adjustments: Seq[GiftCardAdjustment] =
          GiftCardAdjustments.filterByGiftCardId(giftCard1.id).gimme
        adjustments must have size 2
        adjustments.head.state must === (InStorePaymentStates.CancellationCapture)
      }

      "successfully cancels gift card with zero balance" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        GiftCardAdjustments.cancel(adjustment1.id).gimme
        // Update balance
        GiftCards.update(giftCard1, giftCard1.copy(availableBalance = 0)).gimme

        giftCardsApi(giftCard1.code)
          .update(GiftCardUpdateStateByCsr(state = Canceled, reasonId = reason.id.some))
          .as[GiftCardResponse]
          .canceledAmount
          .value must === (0)

        // Ensure that cancel adjustment is automatically created
        val adjustments: Seq[GiftCardAdjustment] =
          GiftCardAdjustments.filterByGiftCardId(giftCard1.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must === (InStorePaymentStates.CancellationCapture)
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
        val adjustments: Seq[GiftCardAdjustmentsResponse] =
          giftCardsApi(giftCard1.code).transactions().as[Seq[GiftCardAdjustmentsResponse]]

        val adjustment: GiftCardAdjustmentsResponse = adjustments.onlyElement
        adjustment.amount must === (-adjustment1.debit)
        adjustment.availableBalance must === (giftCard1.originalBalance - adjustment1.debit)
        adjustment.cordRef.value must === (cart.referenceNumber)
      }
    }

    "PATCH /v1/gift-cards" - {
      "successfully changes statuses of multiple gift cards" in new Fixture {
        private val bothCodes = Seq(giftCard1.code, giftCard2.code)

        private val payload = GiftCardBulkUpdateStateByCsr(
          codes = bothCodes,
          state = GiftCard.OnHold
        )

        giftCardsApi.updateBulk(payload).mustBeOk()

        GiftCards
          .filter(_.code.inSet(bothCodes))
          .map(_.state)
          .gimme must contain only GiftCard.OnHold
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
        val root: ScRoot =
          giftCardsApi(giftCard2.code).convertToStoreCredit(customer.accountId).as[ScRoot]

        root.customerId must === (customer.accountId)
        root.originType must === (StoreCredit.GiftCardTransfer)
        root.state must === (storecredit.StoreCredit.Active)
        root.originalBalance must === (giftCard2.originalBalance)

        val redeemedGc: GiftCard = GiftCards.findByCode(giftCard2.code).one.gimme.value
        redeemedGc.state must === (GiftCard.FullyRedeemed)
        redeemedGc.availableBalance must === (0)
        redeemedGc.currentBalance must === (0)
      }

      "fails to convert when GC not found" in new Fixture {
        giftCardsApi("ABC-666")
          .convertToStoreCredit(customer.accountId)
          .mustFailWith404(NotFoundFailure404(GiftCard, "ABC-666"))
      }

      "fails to convert when customer not found" in new Fixture {
        giftCardsApi(giftCard2.code)
          .convertToStoreCredit(666)
          .mustFailWith404(NotFoundFailure404(User, 666))
      }

      "fails to convert GC to SC if open transactions are present" in new Fixture {
        giftCardsApi(giftCard1.code)
          .convertToStoreCredit(customer.accountId)
          .mustFailWith400(OpenTransactionsFailure)
      }

      "fails to convert inactive GC to SC" in new Fixture {
        GiftCards.findByCode(giftCard2.code).map(_.state).update(GiftCard.OnHold).gimme
        val updatedGc = GiftCards.findByCode(giftCard2.code).one.gimme

        giftCardsApi(giftCard2.code)
          .convertToStoreCredit(customer.accountId)
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
    val adjustment1 = GiftCards.auth(giftCard1, payment.id, 10).gimme
  }
}
