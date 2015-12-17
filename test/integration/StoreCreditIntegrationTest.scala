import Extensions._
import akka.http.scaladsl.model.StatusCodes
import models.StoreCredit._
import models.{Customer, Customers, OrderPayments, Orders, PaymentMethod, Reason, Reasons, StoreAdmins, StoreCredit,
StoreCreditAdjustment, StoreCreditAdjustments, StoreCreditManual, StoreCreditManuals, StoreCreditSubtype,
StoreCreditSubtypes, StoreCredits}
import org.joda.money.CurrencyUnit
import org.scalatest.BeforeAndAfterEach
import responses.{GiftCardResponse, ResponseWithFailuresAndMetadata, StoreCreditAdjustmentsResponse,
StoreCreditResponse, StoreCreditSubTypesResponse}
import services.{EmptyCancellationReasonFailure, InvalidCancellationReasonFailure, NotFoundFailure404,
OpenTransactionsFailure, StoreCreditConvertFailure}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class StoreCreditIntegrationTest extends IntegrationTestBase
  with HttpSupport
  // with SortingAndPaging[responses.StoreCreditResponse.Root]
  with AutomaticAuth
  with BeforeAndAfterEach {

  /*
  // paging and sorting API
  private var currentCustomer: Customer = _
  private var currentOrigin: StoreCreditManual = _

  override def beforeSortingAndPaging() = {
    (for {
      admin    ← * <~ StoreAdmins.create(authedStoreAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      scReason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      scOrigin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = scReason.id))
    } yield (customer, scOrigin)).runT().futureValue.rightVal match {
      case (cc, co) ⇒
        currentCustomer = cc
        currentOrigin = co
    }
  }

  def uriPrefix = s"v1/customers/${currentCustomer.id}/payment-methods/store-credit"

  val regCurrencies = CurrencyUnit.registeredCurrencies.asScala.toIndexedSeq

  def responseItems = {
    val insertScs = regCurrencies.take(numOfResults).map { currency ⇒
      val balance = Random.nextInt(9999999)

      Factories.storeCredit.copy(
        currency = currency,
        originId = currentOrigin.id,
        customerId = currentCustomer.id,
        originalBalance = balance,
        currentBalance = balance,
        availableBalance = balance)
    }

    val result = (for {
      _       ← StoreCredits ++= insertScs
      scs     ← StoreCredits.result
      totals  ← services.StoreCreditService.fetchTotalsForCustomer(currentCustomer.id)
    } yield (responses.StoreCreditResponse.build(scs), totals)).map { case (scs, totals) ⇒
      responses.StoreCreditResponse.WithTotals(scs, totals)
    }

    result.transactionally.run().futureValue.storeCredits.toIndexedSeq
  }

  val sortColumnName = "currency"

  def responseItemsSort(items: IndexedSeq[StoreCreditResponse.Root]) = items.sortBy(_.currency)

  def mf = implicitly[scala.reflect.Manifest[StoreCreditResponse.Root]]
  // paging and sorting API end
  */

  "StoreCredits" - {
    "GET /v1/store-credits/types" - {
      "should return all SC types and related subtypes" in new Fixture {
        val response = GET(s"v1/store-credits/types")
        response.status must ===(StatusCodes.OK)

        val root = response.as[Seq[StoreCreditSubTypesResponse.Root]]
        root.size must === (StoreCredit.OriginType.types.size)
        root.map(_.originType) must === (StoreCredit.OriginType.types.toSeq)
        root.filter(_.originType == scSubType.originType).head.subTypes must === (Seq(scSubType))
      }
    }

    "POST /v1/customers/:id/payment-methods/store-credit" - {
      "when successful" - {
        "responds with the new storeCredit" in new Fixture {
          val payload = payloads.CreateManualStoreCredit(amount = 25, reasonId = scReason.id)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/store-credit", payload)
          response.status must === (StatusCodes.OK)

          val sc = response.as[responses.StoreCreditResponse.Root]
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
        response.status must === (StatusCodes.OK)

        val sc = response.as[responses.StoreCreditResponse.Root]
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

        val result = response.as[ResponseWithFailuresAndMetadata[StoreCreditResponse.WithTotals]].result
        result.storeCredits.map(_.id).sorted must ===(storeCredits.map(_.id).sorted)
        result.totals must not be 'empty
      }

      // FIXME
      "returns not found when customer doesn't exist" in new Fixture {
        pending
        val notFoundResponse = GET(s"v1/customers/99/payment-methods/store-credit")
        notFoundResponse.status must ===(StatusCodes.NotFound)
        notFoundResponse.errors must === (NotFoundFailure404(Customer, 99).description)
      }
    }

    "GET /v1/customers/:id/payment-methods/store-credit/transactions" - {
      "returns list of store credit transactions" in new Fixture {
        val response = GET(s"v1/customers/${customer.id}/payment-methods/store-credit/transactions")
        response.status must ===(StatusCodes.OK)

        val adjustments = response.as[StoreCreditAdjustmentsResponse.Root#ResponseMetadataSeq].result

        adjustments.size must === (1)
        adjustments.headOption.value.id must === (adjustment.id)
      }

      // FIXME
      "returns not found when customer doesn't exist" in new Fixture {
        pending
        val notFoundResponse = GET(s"v1/customers/99/payment-methods/store-credit/transactions")
        notFoundResponse.status must ===(StatusCodes.NotFound)
        notFoundResponse.errors must === (NotFoundFailure404(Customer, 99).description)
      }
    }

    "GET /v1/customers/:id/payment-methods/store-credit/total" - {
      "returns total available and current store credit for customer" in new Fixture {
        val response = GET(s"v1/customers/${customer.id}/payment-methods/store-credit/totals")
        response.status must ===(StatusCodes.OK)

        val totals = response.as[StoreCreditResponse.Totals]

        val fst = StoreCredits.refresh(storeCredit).run().futureValue
        val snd = StoreCredits.refresh(scSecond).run().futureValue

        totals.availableBalance must === (fst.availableBalance + snd.availableBalance)
        totals.currentBalance must === (fst.currentBalance + snd.currentBalance)
      }

      "returns 404 when customer doesn't exist" in new Fixture {
        val response = GET(s"v1/customers/99/payment-methods/store-credit/totals")

        response.status must ===(StatusCodes.NotFound)
        response.errors must === (NotFoundFailure404(Customer, 99).description)
      }
    }

    "GET /v1/store-credits/:id/transactions" - {
      "returns the list of adjustments" in new Fixture {
        val response = GET(s"v1/store-credits/${storeCredit.id}/transactions")
        val adjustments = response.as[StoreCreditAdjustmentsResponse.Root#ResponseMetadataSeq].result

        response.status must ===(StatusCodes.OK)
        adjustments.size must === (1)

        val firstAdjustment = adjustments.head
        firstAdjustment.debit must === (10)
        firstAdjustment.orderRef.value must === (order.referenceNumber)
      }

      "returns the list of adjustments with sorting and paging" in new Fixture {

        val adjustment2 = StoreCredits.auth(storeCredit, Some(payment.id), 1).run().futureValue
        val adjustment3 = StoreCredits.auth(storeCredit, Some(payment.id), 2).run().futureValue

        val response = GET(s"v1/store-credits/${storeCredit.id}/transactions?sortBy=-id&from=2&size=2")
        val adjustments = response.as[StoreCreditAdjustmentsResponse.Root#ResponseMetadataSeq]

        response.status must ===(StatusCodes.OK)
        //adjustments.checkSortingAndPagingMetadata("-id", from = 2, size = 2, resultSize = 1)

        val firstAdjustment = adjustments.result.head
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
        val adjustments = transactionsRep.as[StoreCreditAdjustmentsResponse.Root#ResponseMetadataSeq]
          .result

        response.status must ===(StatusCodes.OK)
        adjustments.size mustBe 2
        adjustments.head.status must ===(StoreCreditAdjustment.CancellationCapture)
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
      admin       ← * <~ StoreAdmins.create(authedStoreAdmin)
      customer    ← * <~ Customers.create(Factories.customer)
      order       ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      scReason    ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      scSubType   ← * <~ StoreCreditSubtypes.create(Factories.storeCreditSubTypes.head)
      scOrigin    ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = scReason.id))
      storeCredit ← * <~ StoreCredits.create(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      scSecond    ← * <~ StoreCredits.create(Factories.storeCredit.copy(originId = scOrigin.id, customerId = customer.id))
      payment     ← * <~ OrderPayments.create(Factories.storeCreditPayment.copy(orderId = order.id,
        paymentMethodId = storeCredit.id, paymentMethodType = PaymentMethod.StoreCredit,
        amount = Some(storeCredit.availableBalance)))
      adjustment ← * <~ StoreCredits.auth(storeCredit, Some(payment.id), 10)
    } yield (admin, customer, scReason, storeCredit, order, adjustment, scSecond, payment, scSubType)).runT()
      .futureValue.rightVal
  }
}

