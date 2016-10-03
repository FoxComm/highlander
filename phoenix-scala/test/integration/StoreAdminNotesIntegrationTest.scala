import java.time.Instant

import failures.NotFoundFailure404
import models.{StoreAdmin, _}
import payloads.NotePayloads._
import responses.AdminNotes
import responses.AdminNotes.Root
import services.notes.StoreAdminNoteManager
import testutils.Extensions._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.time.RichInstant

class StoreAdminNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/notes/store-admins/:adminId" - {
    "can be created by an admin for a customer" in new Fixture {
      val note = notesApi.storeAdmin(storeAdmin.id).create(CreateNote("foo")).as[Root]
      note.body must === ("foo")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      notesApi
        .storeAdmin(storeAdmin.id)
        .create(CreateNote(""))
        .mustFailWithMessage("body must not be empty")
    }

    "returns a 404 if the store admin is not found" in new Fixture {
      notesApi
        .storeAdmin(999999)
        .create(CreateNote(""))
        .mustFailWith404(NotFoundFailure404(StoreAdmin, 999999))
    }
  }

  "GET /v1/notes/store-admins/:adminId" - {

    "can be listed" in new Fixture {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        StoreAdminNoteManager.create(storeAdmin.id, storeAdmin, CreateNote(body))
      }
      DbResultT.sequence(createNotes).gimme

      val notes = notesApi.storeAdmin(storeAdmin.id).get().as[Seq[Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/store-admins/:adminId/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote =
        StoreAdminNoteManager.create(storeAdmin.id, storeAdmin, CreateNote("foo")).gimme

      notesApi
        .storeAdmin(storeAdmin.id)
        .note(rootNote.id)
        .update(UpdateNote("donkey"))
        .as[Root]
        .body must === ("donkey")
    }
  }

  "DELETE /v1/notes/store-admins/:adminId/:noteId" - {

    "can soft delete note" in new Fixture {
      val note = notesApi.storeAdmin(storeAdmin.id).create(CreateNote("foo")).as[Root]

      notesApi.storeAdmin(storeAdmin.id).note(note.id).delete().mustBeEmpty()

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value must === (1)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow mustBe true
      }

      val allNotes = notesApi.storeAdmin(storeAdmin.id).get().as[Seq[Root]]
      allNotes.map(_.id) must not contain note.id
    }
  }

  trait Fixture extends StoreAdmin_Seed
}
