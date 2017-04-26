import java.time.Instant

import failures.{GeneralFailure, NotFoundFailure404}
import models._
import models.returns._
import payloads.NotePayloads._
import responses.AdminNotes
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.{BakedFixtures, ReturnsFixtures}
import utils.time.RichInstant

class ReturnNotesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with ReturnsFixtures
    with BakedFixtures {

  private[this] def api(ref: String) = notesApi.returns(ref)

  "Return Notes" - {
    "POST /v1/notes/return/:code" - {
      "can be created for return" in new ReturnDefaults {
        val note = api(rma.referenceNumber)
          .create(CreateNote(body = "Hello, FoxCommerce!"))
          .as[AdminNotes.Root]

        note.author.name.value must === (defaultAdmin.name.value)
        note.author.email.value must === (defaultAdmin.email.value)
      }

      "returns a validation error if failed to create" in new ReturnDefaults {
        api(rma.referenceNumber)
          .create(CreateNote(body = ""))
          .mustFailWith400(GeneralFailure("body must not be empty"))
      }

      "returns a 404 if the return is not found" in new ReturnDefaults {
        val none = "RMA-666"
        api(none).create(CreateNote(body = "")).mustFailWith404(NotFoundFailure404(Return, none))
      }
    }

    "GET /v1/notes/return/:code" - {

      "can be listed" in new ReturnDefaults {
        val bodies = List("abc", "123", "xyz")

        bodies.foreach { body ⇒
          api(rma.referenceNumber).create(CreateNote(body = body)).mustBeOk()
        }

        api(rma.referenceNumber)
          .get()
          .as[Seq[AdminNotes.Root]]
          .map(_.body) must contain theSameElementsAs bodies
      }
    }

    "PATCH /v1/notes/return/:code/:noteId" - {

      "can update the body text" in new ReturnDefaults {
        val noteCreated = api(rma.referenceNumber)
          .create(CreateNote(body = "Hello, FoxCommerce!"))
          .as[AdminNotes.Root]

        val updateNote = UpdateNote(body = "donkey")
        api(rma.referenceNumber)
          .note(noteCreated.id)
          .update(updateNote)
          .as[AdminNotes.Root]
          .body must === (updateNote.body)
      }
    }

    "DELETE /v1/notes/return/:code/:noteId" - {

      "can soft delete note" in new ReturnDefaults {
        val note = api(rma.referenceNumber)
          .create(CreateNote(body = "Hello, FoxCommerce!"))
          .as[AdminNotes.Root]

        api(rma.referenceNumber).note(note.id).delete().mustBeEmpty()

        val updatedNote = Notes.findOneById(note.id).gimme.value
        updatedNote.deletedBy.value must === (defaultAdmin.id)

        withClue(updatedNote.deletedAt.value → Instant.now) {
          updatedNote.deletedAt.value.isBeforeNow mustBe true
        }

        api(rma.referenceNumber).get().as[Seq[AdminNotes.Root]].map(_.id) must not contain note.id
      }
    }
  }

}
