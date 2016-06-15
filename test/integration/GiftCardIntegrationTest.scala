import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.GiftCardFailures.GiftCardConvertFailure
import failures._
import models.customer.{Customer, Customers}
import models.order.{OrderPayments, Orders}
import models.payment.giftcard.GiftCard._
import models.payment.giftcard._
import models.payment.storecredit.StoreCredit
import models.payment.{PaymentMethod, storecredit}
import models.{Reason, Reasons, StoreAdmins}
import payloads.GiftCardPayloads._
import responses.{GiftCardAdjustmentsResponse, GiftCardBulkResponse, GiftCardResponse, StoreCreditResponse}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Money._
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories

class GiftCardIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GiftCards" - {

    "POST /v1/gift-cards" - {
      "successfully creates gift card from payload" in new Fixture {
        val response = POST(s"v1/gift-cards", GiftCardCreateByCsr(balance = 555, reasonId = 1))
        response.status must ===(StatusCodes.OK)

        val root = response.as[GiftCardResponse.Root]
        root.originType must ===(GiftCard.CsrAppeasement)
        root.currency must ===(Currency.USD)
        root.availableBalance must ===(555)

        // Check that proper link is created
        val manual = GiftCardManuals.findOneById(root.originId).run().futureValue.value
        manual.reasonId must ===(1)
        manual.adminId must ===(admin.id)
      }

      "create two gift cards with unique codes" in new Fixture {
        val payload = GiftCardCreateByCsr(balance = 555, reasonId = 1)

        val responseFirst = POST(s"v1/gift-cards", payload)
        responseFirst.status must ===(StatusCodes.OK)

        val responseSecond = POST(s"v1/gift-cards", payload)
        responseSecond.status must ===(StatusCodes.OK)

        val rootFirst  = responseFirst.as[GiftCardResponse.Root]
        val rootSecond = responseSecond.as[GiftCardResponse.Root]
        rootFirst.code must !==(rootSecond.code)
      }

      "succeeds with valid subTypeId" in new Fixture {
        val payload  = GiftCardCreateByCsr(balance = 25, reasonId = 1, subTypeId = Some(1))
        val response = POST(s"v1/gift-cards", payload)
        val sc       = response.as[GiftCardResponse.Root]

        response.status must ===(StatusCodes.OK)
        sc.subTypeId must ===(Some(1))
      }

      "fails if subtypeId is not found" in new Fixture {
        val payload = GiftCardCreateByCsr(balance = 25,
                                          reasonId = 1,
                                          subTypeId = Some(255))
        val response = POST(s"v1/gift-cards", payload)

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotFoundFailure404(GiftCardSubtype, 255).description)
      }

      "fails to create gift card with negative balance" in new Fixture {
        val response = POST(s"v1/gift-cards", GiftCardCreateByCsr(balance = -555, reasonId = 1))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(
            GeneralFailure("Balance got -555, expected more than 0").description)
      }

      "fails to create gift card with invalid reason" in new Fixture {
        val response = POST(s"v1/gift-cards", GiftCardCreateByCsr(balance = 555, reasonId = 999))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotFoundFailure404(Reason, 999).description)
      }
    }

    "POST /v1/gift-cards (bulk)" - {
      "successfully creates multiple gift cards from payload" in new Fixture {
        val response = POST(s"v1/gift-cards",
                            GiftCardBulkCreateByCsr(quantity = 5,
                                                    balance = 256,
                                                    reasonId = 1))
        response.status must ===(StatusCodes.OK)

        val root = response.as[Seq[GiftCardBulkResponse.ItemResult]]
        root.length must ===(5)
      }

      "fails to create multiple gift cards with zero balance" in new Fixture {
        val response = POST(s"v1/gift-cards",
                            GiftCardBulkCreateByCsr(quantity = 5,
                                                    balance = 0,
                                                    reasonId = 1))

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GeneralFailure("Balance got 0, expected more than 0").description)
      }

      "fails to create multiple gift cards with negative balance" in new Fixture {
        val response = POST(s"v1/gift-cards",
                            GiftCardBulkCreateByCsr(quantity = 5,
                                                    balance = -555,
                                                    reasonId = 1))

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(
            GeneralFailure("Balance got -555, expected more than 0").description)
      }

      "fails to create multiple gift cards with negative quantity" in new Fixture {
        val response = POST(s"v1/gift-cards",
                            GiftCardBulkCreateByCsr(quantity = -5,
                                                    balance = 256,
                                                    reasonId = 1))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(
            GeneralFailure("Quantity got -5, expected more than 0").description)
      }

      "fails to create multiple gift cards with count more than limit" in new Fixture {
        val response = POST(s"v1/gift-cards",
                            GiftCardBulkCreateByCsr(quantity = 25,
                                                    balance = 256,
                                                    reasonId = 1))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GeneralFailure("Quantity got 25, expected 20 or less").description)
      }
    }

    "GET /v1/gift-cards/:code" - {
      "finds a gift card by code" in new Fixture {
        val response     = GET(s"v1/gift-cards/${giftCard.code}")
        val giftCardResp = response.as[GiftCardResponse.Root]

        response.status must ===(StatusCodes.OK)
        giftCardResp.code must ===(giftCard.code)
      }

      "returns not found when GC doesn't exist" in new Fixture {
        val notFoundResponse = GET(s"v1/gift-cards/ABC-666")
        notFoundResponse.status must ===(StatusCodes.NotFound)
        notFoundResponse.error must ===(NotFoundFailure404(GiftCard, "ABC-666").description)
      }
    }

    "PATCH /v1/gift-cards/:code" - {
      "successfully changes state from Active to OnHold and vice-versa" in new Fixture {
        val response =
          PATCH(s"v1/gift-cards/${giftCard.code}", GiftCardUpdateStateByCsr(state = OnHold))
        response.status must ===(StatusCodes.OK)

        val responseBack =
          PATCH(s"v1/gift-cards/${giftCard.code}", GiftCardUpdateStateByCsr(state = Active))
        responseBack.status must ===(StatusCodes.OK)
      }

      "returns error if no cancellation reason provided" in new Fixture {
        val response =
          PATCH(s"v1/gift-cards/${giftCard.code}", GiftCardUpdateStateByCsr(state = Canceled))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(EmptyCancellationReasonFailure.description)
      }

      "returns error on cancellation if gift card has auths" in new Fixture {
        val response = PATCH(s"v1/gift-cards/${giftCard.code}",
                             GiftCardUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(OpenTransactionsFailure.description)
      }

      "successfully cancels gift card with provided reason, cancel adjustment is created" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        GiftCardAdjustments.cancel(adjustment1.id).run().futureValue

        val response = PATCH(s"v1/gift-cards/${giftCard.code}",
                             GiftCardUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
        response.status must ===(StatusCodes.OK)

        val root = response.as[GiftCardResponse.Root]
        root.canceledAmount must ===(Some(giftCard.originalBalance))

        // Ensure that cancel adjustment is automatically created
        val adjustments = GiftCardAdjustments.filterByGiftCardId(giftCard.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must ===(GiftCardAdjustment.CancellationCapture)
      }

      "successfully cancels gift card with zero balance" in new Fixture {
        // Cancel pending adjustment (should be done before cancellation)
        GiftCardAdjustments.cancel(adjustment1.id).run().futureValue
        // Update balance
        GiftCards.update(giftCard, giftCard.copy(availableBalance = 0)).run().futureValue

        val response = PATCH(s"v1/gift-cards/${giftCard.code}",
                             GiftCardUpdateStateByCsr(state = Canceled, reasonId = Some(1)))
        response.status must ===(StatusCodes.OK)

        val root = response.as[GiftCardResponse.Root]
        root.canceledAmount must ===(Some(0))

        // Ensure that cancel adjustment is automatically created
        val adjustments = GiftCardAdjustments.filterByGiftCardId(giftCard.id).gimme
        adjustments.size mustBe 2
        adjustments.head.state must ===(GiftCardAdjustment.CancellationCapture)
      }

      "fails to cancel gift card if invalid reason provided" in new Fixture {
        // Cancel pending adjustment
        GiftCardAdjustments.cancel(adjustment1.id).run().futureValue

        val response = PATCH(s"v1/gift-cards/${giftCard.code}",
                             GiftCardUpdateStateByCsr(state = Canceled, reasonId = Some(999)))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotFoundFailure400(Reason, 999).description)
      }
    }

    "GET /v1/gift-cards/:code/transactions" - {
      "returns the list of adjustments" in new Fixture {
        val response    = GET(s"v1/gift-cards/${giftCard.code}/transactions")
        val adjustments = response.ignoreFailuresAndGiveMe[Seq[GiftCardAdjustmentsResponse.Root]]

        response.status must ===(StatusCodes.OK)
        adjustments.size mustBe 1

        val firstAdjustment = adjustments.head
        firstAdjustment.amount must ===(-adjustment1.debit)
        firstAdjustment.availableBalance must ===(giftCard.originalBalance - adjustment1.debit)
        firstAdjustment.orderRef.value mustBe order.referenceNumber
      }

      "returns the list of adjustments with sorting and paging" in new Fixture {

        val adjustment2 = GiftCards.auth(giftCard, Some(payment.id), 1).gimme
        val adjustment3 = GiftCards.auth(giftCard, Some(payment.id), 2).gimme

        val response    = GET(s"v1/gift-cards/${giftCard.code}/transactions?sortBy=-id&from=2&size=2")
        val adjustments = response.ignoreFailuresAndGiveMe[Seq[GiftCardAdjustmentsResponse.Root]]

        response.status must ===(StatusCodes.OK)
        adjustments.size mustBe 1

        val firstAdjustment = adjustments.head
        firstAdjustment.amount must ===(-adjustment1.debit)
        firstAdjustment.availableBalance must ===(giftCard.originalBalance - adjustment1.debit)
        firstAdjustment.orderRef.value mustBe order.referenceNumber
      }
    }

    "PATCH /v1/gift-cards" - {
      "successfully changes statuses of multiple gift cards" in new Fixture {
        val payload = GiftCardBulkUpdateStateByCsr(
            codes = Seq(giftCard.code, gcSecond.code),
            state = GiftCard.OnHold
        )

        val response = PATCH(s"v1/gift-cards", payload)
        response.status must ===(StatusCodes.OK)

        val firstUpdated = GiftCards.findOneById(giftCard.id).run().futureValue
        firstUpdated.value.state must ===(GiftCard.OnHold)

        val secondUpdated = GiftCards.findOneById(gcSecond.id).run().futureValue
        secondUpdated.value.state must ===(GiftCard.OnHold)
      }

      "returns multiple errors if no cancellation reason provided" in new Fixture {
        val payload = GiftCardBulkUpdateStateByCsr(
            codes = Seq(giftCard.code, gcSecond.code),
            state = GiftCard.Canceled
        )

        val response = PATCH(s"v1/gift-cards", payload)
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(EmptyCancellationReasonFailure.description)
      }
    }

    "POST /v1/gift-cards/:code/convert/:customerId" - {
      "successfully converts GC to SC" in new Fixture {
        val response = POST(s"v1/gift-cards/${gcSecond.code}/convert/${customer.id}")
        response.status must ===(StatusCodes.OK)

        val root = response.as[StoreCreditResponse.Root]
        root.customerId must ===(customer.id)
        root.originType must ===(StoreCredit.GiftCardTransfer)
        root.state must ===(storecredit.StoreCredit.Active)
        root.originalBalance must ===(gcSecond.originalBalance)

        val redeemedGc = GiftCards.findByCode(gcSecond.code).one.run().futureValue.value
        redeemedGc.state must ===(GiftCard.FullyRedeemed)
        redeemedGc.availableBalance must ===(0)
        redeemedGc.currentBalance must ===(0)
      }

      "fails to convert when GC not found" in new Fixture {
        val response = POST(s"v1/gift-cards/ABC-666/convert/${customer.id}")
        response.status must ===(StatusCodes.NotFound)
        response.error must ===(NotFoundFailure404(GiftCard, "ABC-666").description)
      }

      "fails to convert when customer not found" in new Fixture {
        val response = POST(s"v1/gift-cards/${gcSecond.code}/convert/666")
        response.status must ===(StatusCodes.NotFound)
        response.error must ===(NotFoundFailure404(Customer, 666).description)
      }

      "fails to convert GC to SC if open transactions are present" in new Fixture {
        val response = POST(s"v1/gift-cards/${giftCard.code}/convert/${customer.id}")
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(OpenTransactionsFailure.description)
      }

      "fails to convert inactive GC to SC" in new Fixture {
        GiftCards.findByCode(gcSecond.code).map(_.state).update(GiftCard.OnHold).run().futureValue
        val updatedGc = GiftCards.findByCode(gcSecond.code).one.run().futureValue

        val response = POST(s"v1/gift-cards/${gcSecond.code}/convert/${customer.id}")
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GiftCardConvertFailure(updatedGc.value).description)
      }
    }
  }

  trait Fixture {
    val (customer,
         admin,
         giftCard,
         order,
         payment,
         adjustment1,
         gcSecond,
         gcSubType) = (for {
      customer  ← * <~ Customers.create(Factories.customer)
      order     ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      admin     ← * <~ StoreAdmins.create(authedStoreAdmin)
      reason    ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      gcSubType ← * <~ GiftCardSubtypes.create(Factories.giftCardSubTypes.head)
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = admin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
      gcSecond ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id,
                                                               state = GiftCard.Active,
                                                               code = "ABC-234"))
      payment ← * <~ OrderPayments.create(
                   Factories.giftCardPayment.copy(orderId = order.id,
                                                  paymentMethodId = giftCard.id,
                                                  paymentMethodType = PaymentMethod.GiftCard,
                                                  amount = Some(25)))
      adj1     ← * <~ GiftCards.auth(giftCard, Some(payment.id), 10)
      giftCard ← * <~ GiftCards.findOneById(giftCard.id).toXor
    } yield
      (customer,
       admin,
       giftCard.value,
       order,
       payment,
       adj1,
       gcSecond,
       gcSubType)).gimme
  }
}
