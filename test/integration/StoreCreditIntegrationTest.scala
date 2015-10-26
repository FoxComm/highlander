import scala.concurrent.Future
import scala.util.Random
import scala.collection.JavaConverters._
import akka.http.scaladsl.model.StatusCodes

import models.StoreCredit.{ReturnProcess, GiftCardTransfer, CsrAppeasement}
import models._
import models.StoreCredit.{Canceled, Active, OnHold}
import org.joda.money.CurrencyUnit
import responses._
import org.scalatest.BeforeAndAfterEach
import services._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Money.Currency
import utils.Seeds.Factories
import utils.Slick.implicits._

class StoreCreditIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with SortingAndPaging[responses.StoreCreditResponse.Root]
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  // paging and sorting API
  private var currentCustomer: Customer = _
  private var currentOrigin: StoreCreditManual = _

  override def beforeSortingAndPaging() = {
    (for {
      admin    ← StoreAdmins.save(authedStoreAdmin)
      customer ← Customers.save(Factories.customer)
      scReason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      scOrigin ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = scReason.id))
    } yield (customer, scOrigin)).run().futureValue match {
      case (cc, co) ⇒
        currentCustomer = cc
        currentOrigin = co
    }
  }

  def uriPrefix = s"v1/customers/${currentCustomer.id}/payment-methods/store-credit"

  val regCurrencies = CurrencyUnit.registeredCurrencies.asScala.toIndexedSeq

  def responseItems = {
    val items = regCurrencies.take(30).map { currency ⇒
      val balance = Random.nextInt(9999999)
      val future = StoreCredits.save(Factories.storeCredit.copy(
        currency = currency,
        originId = currentOrigin.id,
        customerId = currentCustomer.id,
        originalBalance = balance,
        currentBalance = balance,
        availableBalance = balance)).run()

      future map { responses.StoreCreditResponse.build }
    }

    Future.sequence(items).futureValue
  }

  val sortColumnName = "currency"

  def responseItemsSort(items: IndexedSeq[responses.StoreCreditResponse.Root]) = items.sortBy(_.currency)

  def mf = implicitly[scala.reflect.Manifest[responses.StoreCreditResponse.Root]]
  // paging and sorting API end

  "StoreCredits" - {
    "GET /v1/gift-cards/types" - {
      "should return all GC types" in new Fixture {
        val response = GET(s"v1/store-credits/types")
        val root = response.as[Seq[StoreCredit.OriginType]]

        response.status must ===(StatusCodes.OK)
        root must ===(Seq(CsrAppeasement, GiftCardTransfer, ReturnProcess))
      }
    }

    "GET /v1/gift-cards/subtypes/:type" - {
      "should return all GC subtypes for csrAppeasement" in new Fixture {
        val response = GET(s"v1/store-credits/subtypes/csrAppeasement")
        val root = response.as[Seq[StoreCreditSubtype]]

        response.status must ===(StatusCodes.OK)
        root.head must ===(scSubType)
      }

      "should return error on invalid subtype" in new Fixture {
        val response = GET(s"v1/gift-cards/subtypes/donkeyAppeasement")

        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(InvalidOriginTypeFailure.description)
      }
    }

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

        response.status must === (StatusCodes.BadRequest)
        response.errors must === (NotFoundFailure404(StoreCreditSubtype, 255).description)
      }

      "fails if the customer is not found" in {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = 1)
        val response = POST(s"v1/customers/99/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.NotFound)
        response.errors must === (NotFoundFailure404(Customer, 99).description)
      }

      "fails if the reason is not found" in new Fixture {
        val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = 255)
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)

        response.status must === (StatusCodes.BadRequest)
        response.errors must === (NotFoundFailure404(Reason, 255).description)
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
        storeCreditResponse.availableBalance must === (40)
      }

      "returns not found when SC doesn't exist" in new Fixture {
        val notFoundResponse = GET(s"v1/store-credits/99")
        notFoundResponse.status must ===(StatusCodes.NotFound)
        notFoundResponse.errors must === (NotFoundFailure404(StoreCredit, 99).description)
      }
    }

    "GET /v1/store-credits/:id/transactions" - {
      "returns the list of adjustments" in new Fixture {
        val response = GET(s"v1/store-credits/${storeCredit.id}/transactions")
        val adjustments = response.as[Seq[StoreCreditAdjustmentsResponse.Root]]

        response.status must ===(StatusCodes.OK)
        adjustments.size must === (1)

        val firstAdjustment = adjustments.head
        firstAdjustment.debit must === (10)
        firstAdjustment.orderRef.value must === (order.referenceNumber)
      }

      "returns the list of adjustments with sorting and paging" in new Fixture {

        val adjustment2 = StoreCredits.auth(storeCredit, Some(payment.id), 1).run().futureValue
        val adjustment3 = StoreCredits.auth(storeCredit, Some(payment.id), 2).run().futureValue

        val response = GET(s"v1/store-credits/${storeCredit.id}/transactions?sortBy=-id&pageNo=2&pageSize=2")
        val adjustments = response.as[Seq[StoreCreditAdjustmentsResponse.Root]]

        response.status must ===(StatusCodes.OK)
        adjustments.size must === (1)

        val firstAdjustment = adjustments.head
        firstAdjustment.debit must === (10)
        firstAdjustment.orderRef.value must === (order.referenceNumber)
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
        adjustments.head.state must ===(StoreCreditAdjustment.CancellationCapture)
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


    "POST /v1/customers/:customerId/payment-methods/store-credit/:id/convert" - {
      "successfully converts SC to GC" in new Fixture {
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit/${scSecond.id}/convert")
        response.status must ===(StatusCodes.OK)

        val root = response.as[GiftCardResponse.Root]
        root.originType       must ===(models.GiftCard.FromStoreCredit)
        root.status           must ===(models.GiftCard.Active)
        root.originalBalance  must ===(scSecond.originalBalance)

        val redeemedSc = StoreCredits.filter(_.id === scSecond.id).one.run().futureValue.value
        redeemedSc.status           must ===(StoreCredit.FullyRedeemed)
        redeemedSc.availableBalance must ===(0)
        redeemedSc.currentBalance   must ===(0)
      }

      "fails to convert when SC not found" in new Fixture {
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit/555/convert")
        response.status must ===(StatusCodes.NotFound)
        response.errors must ===(NotFoundFailure404(StoreCredit, 555).description)
      }

      "fails to convert when customer not found" in new Fixture {
        val response = POST(s"v1/customers/666/payment-methods/store-credit/${scSecond.id}/convert")
        response.status must ===(StatusCodes.NotFound)
        response.errors must ===(NotFoundFailure404(Customer, 666).description)
      }

      "fails to convert SC to GC if open transactions are present" in new Fixture {
        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit/${storeCredit.id}/convert")
        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(OpenTransactionsFailure.description)
      }

      "fails to convert inactive SC to GC" in new Fixture {
        StoreCredits.findActiveById(scSecond.id).map(_.status).update(StoreCredit.OnHold).run().futureValue
        val updatedSc = StoreCredits.findActiveById(scSecond.id).one.run().futureValue.value

        val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit/${scSecond.id}/convert")
        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(StoreCreditConvertFailure(updatedSc).description)
      }
    }
  }

  trait Fixture {
    val (admin, customer, scReason, storeCredit, order, adjustment, scSecond, payment, scSubType) = (for {
      admin       ← StoreAdmins.save(authedStoreAdmin)
      customer    ← Customers.save(Factories.customer)
      order       ← Orders.save(Factories.order.copy(customerId = customer.id))
      scReason    ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      scSubType   ← StoreCreditSubtypes.save(Factories.storeCreditSubTypes.head)
      scOrigin    ← StoreCreditManuals.save(Factories.storeCreditManual.copy(adminId = admin.id,
        reasonId = scReason.id))
      storeCredit ← StoreCredits.save(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      scSecond ← StoreCredits.save(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      payment ← OrderPayments.save(Factories.storeCreditPayment.copy(orderId = order.id,
        paymentMethodId = storeCredit.id, paymentMethodType = PaymentMethod.StoreCredit))
      adjustment ← StoreCredits.auth(storeCredit, Some(payment.id), 10)
    } yield (admin, customer, scReason, storeCredit, order, adjustment, scSecond, payment, scSubType)).run().futureValue
  }
}

