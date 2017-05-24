import java.time.Instant

import core.failures.NotFoundFailure404
import phoenix.models.Notes
import phoenix.models.account._
import phoenix.payloads.NotePayloads._
import phoenix.responses.AdminNotes
import phoenix.responses.AdminNotes.Root
import phoenix.utils.time.RichInstant
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.db._

class StoreAdminNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/notes/store-admins/:adminId" - {
    "can be created by an admin for a customer" in new Fixture {
      val note = notesApi.storeAdmin(storeAdmin.accountId).create(CreateNote("foo")).as[Root]
      note.body must === ("foo")
      note.author.name.value must === (defaultAdmin.name.value)
      note.author.email.value must === (defaultAdmin.email.value)
    }

    "returns a validation error if failed to create" in new Fixture {
      notesApi
        .storeAdmin(storeAdmin.accountId)
        .create(CreateNote(""))
        .mustFailWithMessage("body must not be empty")
    }

    "returns a 404 if the store admin is not found" in new Fixture {
      notesApi
        .storeAdmin(999999)
        .create(CreateNote(""))
        .mustFailWith404(NotFoundFailure404(User, 999999))
    }
  }

  "GET /v1/notes/store-admins/:adminId" - {

    "can be listed" in new Fixture {
      val bodies = List("abc", "123", "xyz")

      bodies.map { body ⇒
        notesApi.storeAdmin(storeAdmin.accountId).create(CreateNote(body)).mustBeOk()
      }

      notesApi
        .storeAdmin(storeAdmin.accountId)
        .get()
        .as[Seq[Root]]
        .map(_.body) must contain theSameElementsAs bodies
    }
  }

  "PATCH /v1/notes/store-admins/:adminId/:noteId" - {

    "can update the body text" in new Fixture {
      val note =
        notesApi.storeAdmin(storeAdmin.accountId).create(CreateNote("foo")).as[AdminNotes.Root]

      notesApi
        .storeAdmin(storeAdmin.accountId)
        .note(note.id)
        .update(UpdateNote("donkey"))
        .as[Root]
        .body must === ("donkey")
    }
  }

  "DELETE /v1/notes/store-admins/:adminId/:noteId" - {

    "can soft delete note" in new Fixture {
      val note = notesApi.storeAdmin(storeAdmin.accountId).create(CreateNote("foo")).as[Root]

      notesApi.storeAdmin(storeAdmin.accountId).note(note.id).delete().mustBeEmpty()

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value must === (defaultAdmin.id)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow mustBe true
      }

      val allNotes = notesApi.storeAdmin(storeAdmin.accountId).get().as[Seq[Root]]
      allNotes.map(_.id) must not contain note.id
    }
  }

  trait Fixture extends StoreAdmin_Seed
}
