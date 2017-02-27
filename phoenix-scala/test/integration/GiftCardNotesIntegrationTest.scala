import java.time.Instant

import cats.implicits._
import failures.NotFoundFailure404
import models._
import models.payment.giftcard._
import payloads.NotePayloads._
import responses.AdminNotes
import responses.AdminNotes.Root
import services.notes.GiftCardNoteManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
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
      val note = notesApi.giftCard(giftCard.code).create(CreateNote(body = "foo")).as[Root]
      note.body must === ("foo")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      notesApi
        .giftCard(giftCard.code)
        .create(CreateNote(body = ""))
        .mustFailWithMessage("body must not be empty")
    }

    "returns a 404 if the gift card is not found" in new Fixture {
      notesApi
        .giftCard("NOPE")
        .create(CreateNote(body = ""))
        .mustFailWith404(NotFoundFailure404(GiftCard, "NOPE"))
    }
  }

  "GET /v1/notes/gift-card/:code" - {

    "can be listed" in new Fixture {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        GiftCardNoteManager.create(giftCard.code, storeAdmin, CreateNote(body = body))
      }
      DbResultT.seqCollectFailures(createNotes).gimme

      val notes = notesApi.giftCard(giftCard.code).get().as[Seq[Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/gift-card/:code/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote =
        GiftCardNoteManager.create(giftCard.code, storeAdmin, CreateNote(body = "foo")).gimme

      val note = notesApi
        .giftCard(giftCard.code)
        .note(rootNote.id)
        .update(UpdateNote(body = "donkey"))
        .as[Root]
      note.body must === ("donkey")
    }
  }

  "DELETE /v1/notes/gift-card/:code/:noteId" - {

    "can soft delete note" in new Fixture {
      val note = notesApi.giftCard(giftCard.code).create(CreateNote(body = "foo")).as[Root]

      notesApi.giftCard(giftCard.code).note(note.id).delete().mustBeEmpty()

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value must === (1)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow must === (true)
      }

      val allNotes = notesApi.giftCard(giftCard.code).get().as[Seq[Root]]
      allNotes.map(_.id) must not contain note.id
    }
  }

  trait Fixture extends Reason_Baked {
    val giftCard = (for {
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
  }
}
