import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import models.{Customers, Reasons, GiftCard, GiftCardAdjustment, GiftCardAdjustments, GiftCardManuals, GiftCards,
Orders, OrderPayments, Note, Notes, PaymentMethod, StoreAdmins}
import org.scalatest.BeforeAndAfterEach
import responses.{AdminNotes, GiftCardResponse, GiftCardAdjustmentsResponse}
import services.NoteManager
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

import time.RichInstant

class GiftCardIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "admin API" - {
    "queries the list of gift cards" in new Fixture {
      val response = GET(s"v1/gift-cards")
      val giftCards = Seq(giftCard)

      response.status must ===(StatusCodes.OK)
      val cards = response.as[Seq[GiftCard]]
      cards.map(_.id) must ===(giftCards.map(_.id))
    }

    "finds a gift card by code" in new Fixture {
      val response = GET(s"v1/gift-cards/${giftCard.code}")
      val giftCardResp = response.as[GiftCardResponse.Root]

      response.status must ===(StatusCodes.OK)
      giftCardResp.code must ===(giftCard.code)
    }

    "returns not found when GC doesn't exist" in new Fixture {
      val response = GET(s"v1/gift-cards/somePrefix${giftCard.code}")
      val notFoundResponse = GET(s"v1/gift-cards/99")
      notFoundResponse.status must ===(StatusCodes.NotFound)
    }
  }

  "GET /v1/gift-cards/:code/transactions" - {
    "returns the list of adjustments" in new Fixture {
      val response = GET(s"v1/gift-cards/${giftCard.code}/transactions")
      val adjustments = response.as[Seq[GiftCardAdjustmentsResponse.Root]]

      response.status must ===(StatusCodes.OK)
      adjustments.size mustBe 1

      val firstAdjustment = adjustments.head
      firstAdjustment.amount mustBe -10
      firstAdjustment.availableBalance mustBe 40
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
    val (admin, giftCard) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      admin ← StoreAdmins.save(authedStoreAdmin)
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      giftCard ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
      payment ← OrderPayments.save(Factories.giftCardPayment.copy(orderId = order.id, paymentMethodId = giftCard.id,
        paymentMethodType = PaymentMethod.GiftCard))
      adjustment ← GiftCardAdjustments.save(Factories.giftCardAdjusment.copy(giftCardId = giftCard.id, debit = 10,
        orderPaymentId = payment.id, status = GiftCardAdjustment.Auth))
    } yield (admin, giftCard)).run().futureValue
  }
}
