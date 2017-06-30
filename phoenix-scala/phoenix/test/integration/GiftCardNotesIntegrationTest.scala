import java.time.Instant

import core.failures.NotFoundFailure404
import phoenix.models._
import phoenix.models.payment.giftcard._
import phoenix.payloads.NotePayloads._
import phoenix.responses.AdminNoteResponse
import phoenix.responses.AdminNoteResponse
import phoenix.utils.seeds.Factories
import phoenix.utils.time.RichInstant
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.db._

class GiftCardNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures
    with TestActivityContext.AdminAC {

  "POST /v1/notes/gift-card/:code" - {
    "can be created by an admin for a gift card" in new Fixture {
      val note = notesApi.giftCard(giftCard.code).create(CreateNote(body = "foo")).as[AdminNoteResponse]
      note.body must === ("foo")
      note.author.name.value must === (defaultAdmin.name.value)
      note.author.email.value must === (defaultAdmin.email.value)
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
      val bodies = List("abc", "123", "xyz")

      bodies.foreach { body ⇒
        notesApi.giftCard(giftCard.code).create(CreateNote(body)).mustBeOk()
      }

      notesApi
        .giftCard(giftCard.code)
        .get()
        .as[Seq[AdminNoteResponse]]
        .map(_.body) must contain theSameElementsAs bodies
    }
  }

  "PATCH /v1/notes/gift-card/:code/:noteId" - {

    "can update the body text" in new Fixture {
      val note = notesApi.giftCard(giftCard.code).create(CreateNote("foo")).as[AdminNoteResponse]

      notesApi
        .giftCard(giftCard.code)
        .note(note.id)
        .update(UpdateNote(body = "donkey"))
        .as[AdminNoteResponse]
        .body must === ("donkey")
    }
  }

  "DELETE /v1/notes/gift-card/:code/:noteId" - {

    "can soft delete note" in new Fixture {
      val note = notesApi.giftCard(giftCard.code).create(CreateNote(body = "foo")).as[AdminNoteResponse]

      notesApi.giftCard(giftCard.code).note(note.id).delete().mustBeEmpty()

      val updatedNote = Notes.findOneById(note.id).gimme.value
      updatedNote.deletedBy.value must === (defaultAdmin.id)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow must === (true)
      }

      val allNotes = notesApi.giftCard(giftCard.code).get().as[Seq[AdminNoteResponse]]
      allNotes.map(_.id) must not contain note.id
    }
  }

  trait Fixture extends Reason_Baked {
    val giftCard = (for {
      origin ← * <~ GiftCardManuals.create(
                GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
  }
}
