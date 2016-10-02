import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import models._
import models.payment.giftcard._
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.GiftCardNoteManager
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time.RichInstant

class GiftCardNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures
    with TestActivityContext.AdminAC {

  "POST /v1/notes/gift-card/:code" - {
    "can be created by an admin for a gift card" in new Fixture {
      val response =
        notesApi.giftCard(giftCard.code).create(CreateNote(body = "Hello, FoxCommerce!"))

      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must === ("Hello, FoxCommerce!")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = notesApi.giftCard(giftCard.code).create(CreateNote(body = ""))

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("body must not be empty")
    }

    "returns a 404 if the gift card is not found" in new Fixture {
      val response = notesApi.giftCard("NOPE").create(CreateNote(body = ""))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(GiftCard, "NOPE").description)
    }
  }

  "GET /v1/notes/gift-card/:code" - {

    "can be listed" in new Fixture {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        GiftCardNoteManager.create(giftCard.code, storeAdmin, CreateNote(body = body))
      }
      DbResultT.sequence(createNotes).gimme

      val response = notesApi.giftCard(giftCard.code).get()
      response.status must === (StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/gift-card/:code/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote = GiftCardNoteManager
        .create(giftCard.code, storeAdmin, CreateNote(body = "Hello, FoxCommerce!"))
        .gimme

      val response =
        notesApi.giftCard(giftCard.code).note(rootNote.id).update(UpdateNote(body = "donkey"))
      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must === ("donkey")
    }
  }

  "DELETE /v1/notes/gift-card/:code/:noteId" - {

    "can soft delete note" in new Fixture {
      val createResp =
        notesApi.giftCard(giftCard.code).create(CreateNote(body = "Hello, FoxCommerce!"))
      val note = createResp.as[AdminNotes.Root]

      val response = notesApi.giftCard(giftCard.code).note(note.id).delete()
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value === 1

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow === true
      }

      // Deleted note should not be returned
      val allNotesResponse = notesApi.giftCard(giftCard.code).get()
      allNotesResponse.status must === (StatusCodes.OK)
      val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse = notesApi.giftCard(giftCard.code).note(note.id).get()
      getDeletedNoteResponse.status must === (StatusCodes.NotFound)
    }
  }

  trait Fixture extends Reason_Baked {
    val giftCard = (for {
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
  }
}
