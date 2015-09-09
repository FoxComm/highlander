import akka.http.scaladsl.model.StatusCodes

import models.{Reasons, GiftCard, GiftCardManuals, GiftCards, StoreAdmins}
import org.scalatest.BeforeAndAfterEach
import responses.AdminNotes
import services.NoteManager
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

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
      val giftCardResp = response.as[GiftCard]

      response.status must ===(StatusCodes.OK)
      giftCardResp.code must ===(giftCard.code)

      val notFoundResponse = GET(s"v1/gift-cards/99")
      notFoundResponse.status must ===(StatusCodes.NotFound)
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
  }

  trait Fixture {
    val (admin, giftCard) = (for {
      admin ← StoreAdmins.save(authedStoreAdmin)
      reason ← Reasons.save(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.save(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      giftCard ← GiftCards.save(Factories.giftCard.copy(originId = origin.id))
    } yield (admin, giftCard)).run().futureValue
  }
}
