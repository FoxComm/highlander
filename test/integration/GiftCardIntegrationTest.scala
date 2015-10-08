import java.time.Instant

import scala.concurrent.Await
import akka.http.scaladsl.model.StatusCodes

import models.{Customers, Reasons, GiftCard, GiftCardAdjustment, GiftCardAdjustments, GiftCardManuals, GiftCards,
Orders, OrderPayments, Note, Notes, PaymentMethod, StoreAdmins}
import models.GiftCard.{Active, OnHold, Canceled}
import org.scalatest.BeforeAndAfterEach
import responses.{AdminNotes, GiftCardResponse, GiftCardAdjustmentsResponse}
import services.NoteManager
import services._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import utils.time.RichInstant
import utils.Money._

class GiftCardIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "GET /v1/gift-cards" - {
    "returns list of gift cards" in new Fixture {
      val response = GET(s"v1/gift-cards")
      val giftCards = Seq(giftCard, gcSecond)

      response.status must ===(StatusCodes.OK)
      val cards = response.as[Seq[GiftCard]]
      cards.map(_.id).sorted must ===(giftCards.map(_.id).sorted)
    }
  }

  "POST /v1/gift-cards" - {
    "successfully creates gift card from payload" in new Fixture {
      val response = POST(s"v1/gift-cards", payloads.GiftCardCreateByCsr(balance = 555))
      val root = response.as[GiftCardResponse.Root]

      response.status must ===(StatusCodes.OK)
      root.originType must ===(GiftCard.CsrAppeasement)
      root.currency must ===(Currency.USD)
      root.availableBalance must ===(555)
    }

    "fails to create gift card with negative balance" in new Fixture {
      val response = POST(s"v1/gift-cards", payloads.GiftCardCreateByCsr(balance = -555))
      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Balance must be greater than zero").description)
    }
  }

  "POST /v1/gift-cards/_bulk" - {
    "successfully creates multiple gift cards from payload" in new Fixture {
      val response = POST(s"v1/gift-cards/_bulk", payloads.GiftCardBulkCreateByCsr(quantity = 5, balance = 256))
      val root = response.as[Seq[GiftCardResponse.Root]]

      response.status must ===(StatusCodes.OK)
      root.length must ===(5)
    }

    "fails to create multiple gift cards with zero balance" in new Fixture {
      val response = POST(s"v1/gift-cards/_bulk", payloads.GiftCardBulkCreateByCsr(quantity = 5, balance = 0))

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Balance must be greater than zero").description)
    }

    "fails to create multiple gift cards with negative balance" in new Fixture {
      val response = POST(s"v1/gift-cards/_bulk", payloads.GiftCardBulkCreateByCsr(quantity = 5, balance = -555))

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Balance must be greater than zero").description)
    }

    "fails to create multiple gift cards with negative quantity" in new Fixture {
      val response = POST(s"v1/gift-cards/_bulk", payloads.GiftCardBulkCreateByCsr(quantity = -5, balance = 256))
      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Quantity must be greater than zero").description)
    }

    "fails to create multiple gift cards with count more than limit" in new Fixture {
      val response = POST(s"v1/gift-cards/_bulk", payloads.GiftCardBulkCreateByCsr(quantity = 25, balance = 256))
      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Bulk creation limit exceeded").description)
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
      notFoundResponse.errors must ===(GiftCardNotFoundFailure("ABC-666").description)
    }
  }

  "PATCH /v1/gift-cards/:code" - {
    "successfully changes status from Active to OnHold and vice-versa" in new Fixture {
      val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStatusByCsr(status = OnHold))
      response.status must ===(StatusCodes.OK)

      val responseBack = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStatusByCsr(status = Active))
      responseBack.status must ===(StatusCodes.OK)
    }

    "returns error if no cancellation reason provided" in new Fixture {
      val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStatusByCsr(status = Canceled))
      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(EmptyCancellationReasonFailure.description)
    }

    "returns error on cancellation if gift card has auths" in new Fixture {
      val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStatusByCsr(status = Canceled,
        reason = Some(1)))
      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(OpenTransactionsFailure.description)
    }

    "successfully cancels gift card with provided reason, cancel adjustment is created" in new Fixture {
      // Cancel pending adjustment
      GiftCardAdjustments.cancel(adjustment.id).run().futureValue

      val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStatusByCsr(status = Canceled,
        reason = Some(1)))
      response.status must ===(StatusCodes.OK)

      val root = response.as[GiftCardResponse.Root]
      root.canceledAmount must ===(Some(giftCard.originalBalance))

      // Ensure that cancel adjustment is automatically created
      val transactionsRep = GET(s"v1/gift-cards/${giftCard.code}/transactions")
      val adjustments = transactionsRep.as[Seq[GiftCardAdjustmentsResponse.Root]]
      response.status must ===(StatusCodes.OK)
      adjustments.size mustBe 2
      adjustments.head.state must ===(GiftCardAdjustment.Capture)
    }

    "fails to cancel gift card if invalid reason provided" in new Fixture {
      // Cancel pending adjustment
      GiftCardAdjustments.cancel(adjustment.id).run().futureValue

      val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStatusByCsr(status = Canceled,
        reason = Some(999)))
      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(InvalidCancellationReasonFailure.description)
    }
  }

  "GET /v1/gift-cards/:code/transactions" - {
    "returns the list of adjustments" in new Fixture {
      val response = GET(s"v1/gift-cards/${giftCard.code}/transactions")
      val adjustments = response.as[Seq[GiftCardAdjustmentsResponse.Root]]

      response.status must ===(StatusCodes.OK)
      adjustments.size mustBe 1

      val firstAdjustment = adjustments.head
      firstAdjustment.amount must ===(-adjustment.debit)
      firstAdjustment.availableBalance must ===(giftCard.originalBalance - adjustment.debit)
      firstAdjustment.orderRef.value mustBe order.referenceNumber
    }
  }

  "PATCH /v1/gift-cards" - {
    "successfully changes statuses of multiple gift cards" in new Fixture {
      val payload = payloads.GiftCardBulkUpdateStatusByCsr(
        codes = Seq(giftCard.code, gcSecond.code),
        status = GiftCard.OnHold
      )

      val response = PATCH(s"v1/gift-cards", payload)
      response.status must ===(StatusCodes.OK)

      val firstUpdated = GiftCards.findById(giftCard.id).run().futureValue
      firstUpdated.value.status must ===(GiftCard.OnHold)

      val secondUpdated = GiftCards.findById(gcSecond.id).run().futureValue
      secondUpdated.value.status must ===(GiftCard.OnHold)
    }

    "returns multiple errors if no cancellation reason provided" in new Fixture {
      val payload = payloads.GiftCardBulkUpdateStatusByCsr(
        codes = Seq(giftCard.code, gcSecond.code),
        status = GiftCard.Canceled
      )

      val response = PATCH(s"v1/gift-cards", payload)
      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(EmptyCancellationReasonFailure.description)
    }
  }

  "POST /v1/gift-cards/:code/convert/:customerId" - {
    "successfully converts GC to SC" in new Fixture {
      val response = POST(s"v1/gift-cards/${gcSecond.code}/convert/${customer.id}")
      response.status must ===(StatusCodes.OK)

      val root = response.as[models.StoreCredit]
      root.customerId       must ===(customer.id)
      root.originType       must ===(models.StoreCredit.GiftCardTransfer)
      root.status           must ===(models.StoreCredit.Active)
      root.originalBalance  must ===(gcSecond.originalBalance)

      val redeemedGc = GiftCards.findByCode(gcSecond.code).one.run().futureValue.value
      redeemedGc.status           must ===(GiftCard.FullyRedeemed)
      redeemedGc.availableBalance must ===(0)
      redeemedGc.currentBalance   must ===(0)
    }

    "fails to convert when GC not found" in new Fixture {
      val response = POST(s"v1/gift-cards/ABC-666/convert/${customer.id}")
      response.status  must ===(StatusCodes.NotFound)
      response.errors  must ===(GiftCardNotFoundFailure("ABC-666").description)
    }

    "fails to convert when customer not found" in new Fixture {
      val response = POST(s"v1/gift-cards/${gcSecond.code}/convert/666")
      response.status  must ===(StatusCodes.NotFound)
      response.errors  must ===(NotFoundFailure(models.Customer, 666).description)
    }

    "fails to convert inactive GC to SC if open transactions are present" in new Fixture {
      val response = POST(s"v1/gift-cards/${giftCard.code}/convert/${customer.id}")
      response.status  must ===(StatusCodes.BadRequest)
      response.errors  must ===(OpenTransactionsFailure.description)
    }

    "fails to convert inactive GC to SC" in new Fixture {
      GiftCards.findByCode(gcSecond.code).map(_.status).update(GiftCard.OnHold).run().futureValue
      val updatedGc = GiftCards.findByCode(gcSecond.code).one.run().futureValue

      val response = POST(s"v1/gift-cards/${gcSecond.code}/convert/${customer.id}")
      response.status  must ===(StatusCodes.BadRequest)
      response.errors  must ===(GiftCardConvertFailure(updatedGc.value).description)
    }
  }

  "gift card note" - {
    "can be created by an admin for a gift card" in new Fixture {
      val response = POST(s"v1/notes/gift-card/${giftCard.code}",
        payloads.CreateNote(body = "Hello, FoxCommerce!"))

      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("Hello, FoxCommerce!")
      note.author must ===(AdminNotes.buildAuthor(admin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/notes/gift-card/${giftCard.code}", payloads.CreateNote(body = ""))

      response.status must ===(StatusCodes.BadRequest)
      response.bodyText must include("errors")
    }

    "returns a 404 if the gift card is not found" in new Fixture {
      val response = POST(s"v1/notes/gift-card/999999", payloads.CreateNote(body = ""))

      response.status must ===(StatusCodes.NotFound)
      response.bodyText mustBe 'empty
    }

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        NoteManager.createGiftCardNote(giftCard, admin, payloads.CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/notes/gift-card/${giftCard.code}")
      response.status must ===(StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must ===(Set("abc", "123", "xyz"))
    }

    "can update the body text" in new Fixture {
      val rootNote = NoteManager.createGiftCardNote(giftCard, admin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue.get

      val response = PATCH(s"v1/notes/gift-card/${giftCard.code}/${rootNote.id}", payloads.UpdateNote(body = "donkey"))
      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("donkey")
    }

    "can soft delete note" in new Fixture {
      val createResp = POST(s"v1/notes/gift-card/${giftCard.code}", payloads.CreateNote(body = "Hello, FoxCommerce!"))
      val note = createResp.as[AdminNotes.Root]

      val response = DELETE(s"v1/notes/gift-card/${giftCard.code}/${note.id}")
      response.status must ===(StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = db.run(Notes.findById(note.id)).futureValue.value
      updatedNote.deletedBy.value mustBe 1

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow mustBe true
      }
    }
  }

  trait Fixture {
    val (customer, admin, giftCard, order, adjustment, gcSecond) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      admin ← StoreAdmins.save(authedStoreAdmin)
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      giftCard ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active))
      gcSecond ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active,
        code = "ABC-234"))
      payment ← OrderPayments.save(Factories.giftCardPayment.copy(orderId = order.id, paymentMethodId = giftCard.id,
        paymentMethodType = PaymentMethod.GiftCard))
      adjustment ← GiftCards.auth(giftCard, Some(payment.id), 10)
      giftCard ← GiftCards.findById(giftCard.id)
    } yield (customer, admin, giftCard.value, order, adjustment, gcSecond)).run().futureValue
  }
}
