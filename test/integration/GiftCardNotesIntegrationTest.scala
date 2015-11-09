import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models.{Notes, _}
import responses.AdminNotes
import services.{NotFoundFailure404, NoteManager}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import utils.time.RichInstant
import scala.concurrent.ExecutionContext.Implicits.global

class GiftCardNotesIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST /v1/notes/gift-card/:code" - {
    "can be created by an admin for a gift card" in new Fixture {
      val response = POST(s"v1/notes/gift-card/${giftCard.code}",
        payloads.CreateNote(body = "Hello, FoxCommerce!"))

      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must === ("Hello, FoxCommerce!")
      note.author must === (AdminNotes.buildAuthor(admin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/notes/gift-card/${giftCard.code}", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.BadRequest)
      response.errors must === (List("body must not be empty"))
    }

    "returns a 404 if the gift card is not found" in new Fixture {
      val response = POST(s"v1/notes/gift-card/999999", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.NotFound)
      parseErrors(response) must === (NotFoundFailure404(GiftCard, 999999).description)
    }
  }

  "GET /v1/notes/gift-card/:code" - {

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        NoteManager.createGiftCardNote(giftCard.code, admin, payloads.CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/notes/gift-card/${giftCard.code}")
      response.status must === (StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/gift-card/:code/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote = rightValue(NoteManager.createGiftCardNote(giftCard.code, admin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue)

      val response = PATCH(s"v1/notes/gift-card/${giftCard.code}/${rootNote.id}", payloads.UpdateNote(body = "donkey"))
      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must === ("donkey")
    }
  }

  "DELETE /v1/notes/gift-card/:code/:noteId" - {

    "can soft delete note" in new Fixture {
      val createResp = POST(s"v1/notes/gift-card/${giftCard.code}", payloads.CreateNote(body = "Hello, FoxCommerce!"))
      val note = createResp.as[AdminNotes.Root]

      val response = DELETE(s"v1/notes/gift-card/${giftCard.code}/${note.id}")
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value === (1)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow === (true)
      }

      // Deleted note should not be returned
      val allNotesResponse = GET(s"v1/notes/gift-card/${giftCard.code}")
      allNotesResponse.status must === (StatusCodes.OK)
      val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse = GET(s"v1/notes/gift-card/${giftCard.code}/${note.id}")
      getDeletedNoteResponse.status must === (StatusCodes.NotFound)
    }
  }

  trait Fixture {
    val (admin, giftCard) = (for {
      admin ← StoreAdmins.saveNew(authedStoreAdmin)
      reason ← Reasons.saveNew(Factories.reason.copy(storeAdminId = admin.id))
      origin ← GiftCardManuals.saveNew(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
      giftCard ← GiftCards.saveNew(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active))
    } yield (admin, giftCard)).run().futureValue
  }

}
