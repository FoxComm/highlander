import Extensions._
import akka.http.scaladsl.model.StatusCodes
import models.GiftCard._
import models.{Customers, GiftCard, GiftCardAdjustment, GiftCardAdjustments, GiftCardManual, GiftCardManuals,
GiftCardSubtype, GiftCardSubtypes, GiftCards, OrderPayments, Orders, PaymentMethod, Reason, Reasons, StoreAdmins}
import org.joda.money.CurrencyUnit
import org.scalatest.BeforeAndAfterEach
import responses.{GiftCardAdjustmentsResponse, GiftCardBulkResponse, GiftCardResponse, GiftCardSubTypesResponse, StoreCreditResponse}
import services.{NotFoundFailure400, EmptyCancellationReasonFailure, GeneralFailure, GiftCardConvertFailure,
NotFoundFailure404, OpenTransactionsFailure}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class GiftCardIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[GiftCardResponse.Root]
  with AutomaticAuth
  with BeforeAndAfterEach {

  // paging and sorting API
  private var currentOrigin: GiftCardManual = _

  override def beforeSortingAndPaging(): Unit = {
    currentOrigin = (for {
      admin ← * <~ StoreAdmins.create(authedStoreAdmin)
      reason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
    } yield origin).runTxn().futureValue.rightVal
  }

  def uriPrefix = "v1/gift-cards"

  val regCurrencies = CurrencyUnit.registeredCurrencies.asScala.toIndexedSeq

  def responseItems = {
    val insertGcs = regCurrencies.take(numOfResults).map { currency ⇒
      val balance = Random.nextInt(9999999)

      Factories.giftCard.copy(
        currency = currency,
        originId = currentOrigin.id,
        originalBalance = balance,
        currentBalance = balance,
        availableBalance = balance)
    }

    (GiftCards.createAll(insertGcs) >> GiftCards.result).map { giftCards ⇒
      giftCards.map(responses.GiftCardResponse.build(_))
    }.transactionally.run().futureValue.toIndexedSeq
  }

  val sortColumnName = "availableBalance"

  def responseItemsSort(items: IndexedSeq[GiftCardResponse.Root]) = items.sortBy(_.availableBalance)

  def mf = implicitly[scala.reflect.Manifest[GiftCardResponse.Root]]
  // paging and sorting API end

  "GiftCards" - {
    "GET /v1/gift-cards/types" - {
      "should return all GC types and related sub-types" in new Fixture {
        val response = GET(s"v1/gift-cards/types")
        response.status must ===(StatusCodes.OK)

        val root = response.as[Seq[GiftCardSubTypesResponse.Root]]
        root.size must === (GiftCard.OriginType.types.size)
        root.map(_.originType) must === (GiftCard.OriginType.types.toSeq)
        root.filter(_.originType == gcSubType.originType).head.subTypes must === (Seq(gcSubType))
      }
    }

    "GET /v1/gift-cards" - {
      "returns list of gift cards" in new Fixture {
        val response = GET(s"v1/gift-cards")
        val giftCards = Seq(giftCard, gcSecond)

        response.status must ===(StatusCodes.OK)
        val resp = response.ignoreFailuresAndGiveMe[Seq[GiftCard]]
        resp.map(_.id).sorted must ===(giftCards.map(_.id).sorted)
      }
    }

    "POST /v1/gift-cards" - {
      "successfully creates gift card from payload" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardCreateByCsr(balance = 555, reasonId = 1))
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
        val payload = payloads.GiftCardCreateByCsr(balance = 555, reasonId = 1)

        val responseFirst = POST(s"v1/gift-cards", payload)
        responseFirst.status must ===(StatusCodes.OK)

        val responseSecond = POST(s"v1/gift-cards", payload)
        responseSecond.status must ===(StatusCodes.OK)

        val rootFirst = responseFirst.as[GiftCardResponse.Root]
        val rootSecond = responseSecond.as[GiftCardResponse.Root]
        rootFirst.code must !==(rootSecond.code)
      }

      "succeeds with valid subTypeId" in new Fixture {
        val payload = payloads.GiftCardCreateByCsr(balance = 25, reasonId = 1, subTypeId = Some(1))
        val response = POST(s"v1/gift-cards", payload)
        val sc = response.as[responses.GiftCardResponse.Root]

        response.status must ===(StatusCodes.OK)
        sc.subTypeId must ===(Some(1))
      }

      "fails if subtypeId is not found" in new Fixture {
        val payload = payloads.GiftCardCreateByCsr(balance = 25, reasonId = 1, subTypeId = Some(255))
        val response = POST(s"v1/gift-cards", payload)

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotFoundFailure404(GiftCardSubtype, 255).description)
      }

      "fails to create gift card with negative balance" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardCreateByCsr(balance = -555, reasonId = 1))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GeneralFailure("Balance got -555, expected more than 0").description)
      }

      "fails to create gift card with invalid reason" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardCreateByCsr(balance = 555, reasonId = 999))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotFoundFailure404(Reason, 999).description)
      }
    }

    "POST /v1/gift-cards (bulk)" - {
      "successfully creates multiple gift cards from payload" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardBulkCreateByCsr(quantity = 5, balance = 256,
          reasonId = 1))
        response.status must ===(StatusCodes.OK)

        val root = response.as[Seq[GiftCardBulkResponse.ItemResult]]
        root.length must ===(5)
      }

      "fails to create multiple gift cards with zero balance" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardBulkCreateByCsr(quantity = 5, balance = 0,
          reasonId = 1))

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GeneralFailure("Balance got 0, expected more than 0").description)
      }

      "fails to create multiple gift cards with negative balance" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardBulkCreateByCsr(quantity = 5, balance = -555,
          reasonId = 1))

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GeneralFailure("Balance got -555, expected more than 0").description)
      }

      "fails to create multiple gift cards with negative quantity" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardBulkCreateByCsr(quantity = -5, balance = 256,
          reasonId = 1))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GeneralFailure("Quantity got -5, expected more than 0").description)
      }

      "fails to create multiple gift cards with count more than limit" in new Fixture {
        val response = POST(s"v1/gift-cards", payloads.GiftCardBulkCreateByCsr(quantity = 25, balance = 256,
          reasonId = 1))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(GeneralFailure("Quantity got 25, expected 20 or less").description)
      }
    }

    "GET /v1/gift-cards/:code" - {
      "finds a gift card by code" in new Fixture {
        val response = GET(s"v1/gift-cards/${giftCard.code}")
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
        val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStateByCsr(state = OnHold))
        response.status must ===(StatusCodes.OK)

        val responseBack = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStateByCsr(state = Active))
        responseBack.status must ===(StatusCodes.OK)
      }

      "returns error if no cancellation reason provided" in new Fixture {
        val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStateByCsr(state = Canceled))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(EmptyCancellationReasonFailure.description)
      }

      "returns error on cancellation if gift card has auths" in new Fixture {
        val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStateByCsr(state = Canceled,
          reasonId = Some(1)))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(OpenTransactionsFailure.description)
      }

      "successfully cancels gift card with provided reason, cancel adjustment is created" in new Fixture {
        // Cancel pending adjustment
        GiftCardAdjustments.cancel(adjustment1.id).run().futureValue

        val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStateByCsr(state = Canceled,
          reasonId = Some(1)))
        response.status must ===(StatusCodes.OK)

        val root = response.as[GiftCardResponse.Root]
        root.canceledAmount must ===(Some(giftCard.originalBalance))

        // Ensure that cancel adjustment is automatically created
        val transactionsRep = GET(s"v1/gift-cards/${giftCard.code}/transactions")
        val adjustments = transactionsRep.ignoreFailuresAndGiveMe[Seq[GiftCardAdjustmentsResponse.Root]]
        response.status must ===(StatusCodes.OK)
        adjustments.size mustBe 2
        adjustments.head.state must ===(GiftCardAdjustment.CancellationCapture)
      }

      "fails to cancel gift card if invalid reason provided" in new Fixture {
        // Cancel pending adjustment
        GiftCardAdjustments.cancel(adjustment1.id).run().futureValue

        val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStateByCsr(state = Canceled,
          reasonId = Some(999)))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotFoundFailure400(Reason, 999).description)
      }
    }

    "GET /v1/gift-cards/:code/transactions" - {
      "returns the list of adjustments" in new Fixture {
        val response = GET(s"v1/gift-cards/${giftCard.code}/transactions")
        val adjustments = response.ignoreFailuresAndGiveMe[Seq[GiftCardAdjustmentsResponse.Root]]

        response.status must ===(StatusCodes.OK)
        adjustments.size mustBe 1

        val firstAdjustment = adjustments.head
        firstAdjustment.amount must ===(-adjustment1.debit)
        firstAdjustment.availableBalance must ===(giftCard.originalBalance - adjustment1.debit)
        firstAdjustment.orderRef.value mustBe order.referenceNumber
      }

      "returns the list of adjustments with sorting and paging" in new Fixture {

        val adjustment2 = GiftCards.auth(giftCard, Some(payment.id), 1).run().futureValue.rightVal
        val adjustment3 = GiftCards.auth(giftCard, Some(payment.id), 2).run().futureValue.rightVal

        val response = GET(s"v1/gift-cards/${giftCard.code}/transactions?sortBy=-id&from=2&size=2")
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
        val payload = payloads.GiftCardBulkUpdateStateByCsr(
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
        val payload = payloads.GiftCardBulkUpdateStateByCsr(
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
        root.originType must ===(models.StoreCredit.GiftCardTransfer)
        root.state must ===(models.StoreCredit.Active)
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
        response.error must ===(NotFoundFailure404(models.Customer, 666).description)
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
    val (customer, admin, giftCard, order, payment, adjustment1, gcSecond, gcSubType) = (for {
      customer  ← * <~ Customers.create(Factories.customer)
      order     ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      admin     ← * <~ StoreAdmins.create(authedStoreAdmin)
      reason    ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      gcSubType ← * <~ GiftCardSubtypes.create(Factories.giftCardSubTypes.head)
      origin    ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
      giftCard  ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
      gcSecond  ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active,
        code = "ABC-234"))
      payment   ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = order.id, paymentMethodId =
        giftCard.id, paymentMethodType = PaymentMethod.GiftCard, amount = Some(25)))
      adj1      ← * <~ GiftCards.auth(giftCard, Some(payment.id), 10)
      giftCard  ← * <~ GiftCards.findOneById(giftCard.id).toXor
    } yield (customer, admin, giftCard.value, order, payment, adj1, gcSecond, gcSubType)).runTxn().futureValue.rightVal
  }
}
