import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import models.{Customers, Reasons, GiftCard, GiftCardAdjustment, GiftCardAdjustments, GiftCardManuals, GiftCards,
Orders, OrderPayments, Note, Notes, PaymentMethod, StoreAdmins}
import models.GiftCard.{Active, OnHold, Canceled}
import org.scalatest.BeforeAndAfterEach
import responses.{AdminNotes, GiftCardResponse, GiftCardAdjustmentsResponse}
import services.NoteManager
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
      val giftCards = Seq(giftCard)

      response.status must ===(StatusCodes.OK)
      val cards = response.as[Seq[GiftCard]]
      cards.map(_.id) must ===(giftCards.map(_.id))
    }
  }

  "POST /v1/gift-cards" - {
    "successfully creates gift card from payload" in new Fixture {
      val response = POST(s"v1/gift-cards", payloads.GiftCardCreateByCsr(balance = 555))
      val root = response.as[GiftCardResponse.Root]

      response.status must ===(StatusCodes.OK)
      root.`type` must ===(GiftCard.CsrAppeasement)
      root.currency must ===(Currency.USD)
      root.availableBalance must ===(555)
    }

    "fails to create gift card with negative balance" in new Fixture {
      val response = POST(s"v1/gift-cards", payloads.GiftCardCreateByCsr(balance = -555))
      response.status must ===(StatusCodes.BadRequest)
      response.errors.head must ===("originalBalance should be greater or equal than zero")
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
      val notFoundResponse = GET(s"v1/gift-cards/99")
      notFoundResponse.status must ===(StatusCodes.NotFound)
      notFoundResponse.errors.head must ===("giftCard with code=99 not found")
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
      response.errors.head must ===("Please provide valid cancellation reason")
    }

    "returns error on cancellation if gift card has auths" in new Fixture {
      val response = PATCH(s"v1/gift-cards/${giftCard.code}", payloads.GiftCardUpdateStatusByCsr(status = Canceled,
        reason = Some(1)))
      response.status must ===(StatusCodes.BadRequest)
      response.errors.head must ===("Open transactions should be canceled/completed")
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
      response.errors.head must ===("Cancellation reason doesn't exist")
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
      firstAdjustment.orderRef.get mustBe order.referenceNumber
    }
  }

  "gift card note" - {
    "can be created by an admin for a gift card" in new Fixture {
      val response = POST(s"v1/gift-cards/${giftCard.code}/notes",
        payloads.CreateNote(body = "Hello, FoxCommerce!"))

      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("Hello, FoxCommerce!")
      note.author must ===(AdminNotes.buildAuthor(admin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/gift-cards/${giftCard.code}/notes", payloads.CreateNote(body = ""))

      response.status must ===(StatusCodes.BadRequest)
      response.bodyText must include("errors")
    }

    "returns a 404 if the gift card is not found" in new Fixture {
      val response = POST(s"v1/gift-cards/999999/notes", payloads.CreateNote(body = ""))

      response.status must ===(StatusCodes.NotFound)
      response.bodyText mustBe 'empty
    }

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        NoteManager.createGiftCardNote(giftCard, admin, payloads.CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/gift-cards/${giftCard.code}/notes")
      response.status must ===(StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must ===(Set("abc", "123", "xyz"))
    }

    "can update the body text" in new Fixture {
      val rootNote = NoteManager.createGiftCardNote(giftCard, admin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue.get

      val response = PATCH(s"v1/gift-cards/${giftCard.code}/notes/${rootNote.id}", payloads.UpdateNote(body = "donkey"))
      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("donkey")
    }

    "can soft delete note" in new Fixture {
      val note = NoteManager.createGiftCardNote(giftCard, admin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue.get
      StoreAdmins.save(Factories.storeAdmin).run().futureValue

      val response = DELETE(s"v1/gift-cards/${giftCard.id}/notes/${note.id}")
      response.status must ===(StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = db.run(Notes.findById(note.id)).futureValue.get
      updatedNote.deletedBy.get mustBe 1

      withClue(updatedNote.deletedAt.get → Instant.now) {
        updatedNote.deletedAt.get.isBeforeNow mustBe true
      }
    }
  }

  trait Fixture {
    val (admin, giftCard, order, adjustment) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      admin ← StoreAdmins.save(authedStoreAdmin)
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      giftCard ← GiftCards.save(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active))
      payment ← OrderPayments.save(Factories.giftCardPayment.copy(orderId = order.id, paymentMethodId = giftCard.id,
        paymentMethodType = PaymentMethod.GiftCard))
      adjustment ← GiftCards.auth(giftCard, Some(payment.id), 10)
      giftCard ← GiftCards.findById(giftCard.id)
    } yield (admin, giftCard.get, order, adjustment)).run().futureValue
  }
}
