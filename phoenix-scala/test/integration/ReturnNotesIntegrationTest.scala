import java.time.Instant

import failures.{GeneralFailure, NotFoundFailure404}
import models._
import models.returns._
import payloads.NotePayloads._
import responses.AdminNotes
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time.RichInstant

class ReturnNotesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  private[this] def api(ref: String) = notesApi.returns(ref)

  "Return Notes" - {
    "POST /v1/notes/return/:code" - {
      "can be created for return" in new Fixture {
        val noteCreated =
          api(rma.refNum).create(CreateNote(body = "Hello, FoxCommerce!")).as[AdminNotes.Root]

        noteCreated.body must === ("Hello, FoxCommerce!")
        noteCreated.author must === (AdminNotes.buildAuthor(storeAdmin))
      }

      "returns a validation error if failed to create" in new Fixture {
        api(rma.refNum)
          .create(CreateNote(body = ""))
          .mustFailWith400(GeneralFailure("body must not be empty"))
      }

      "returns a 404 if the return is not found" in new Fixture {
        private val none = "RMA-666"
        api(none).create(CreateNote(body = "")).mustFailWith404(NotFoundFailure404(Return, none))
      }
    }

    "GET /v1/notes/return/:code" - {

      "can be listed" in new Fixture {
        List("abc", "123", "xyz").foreach { body ⇒
          api(rma.refNum).create(CreateNote(body = body)).mustBeOk()
        }

        val notes = api(rma.refNum).get().as[Seq[AdminNotes.Root]]
        notes must have size 3
        notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
      }
    }

    "PATCH /v1/notes/return/:code/:noteId" - {

      "can update the body text" in new Fixture {
        val noteCreated =
          api(rma.refNum).create(CreateNote(body = "Hello, FoxCommerce!")).as[AdminNotes.Root]

        val updateNote = UpdateNote(body = "donkey")
        api(rma.refNum).note(noteCreated.id).update(updateNote).as[AdminNotes.Root].body must === (
            updateNote.body)
      }
    }

    "DELETE /v1/notes/return/:code/:noteId" - {

      "can soft delete note" in new Fixture {
        val note =
          api(rma.refNum).create(CreateNote(body = "Hello, FoxCommerce!")).as[AdminNotes.Root]

        api(rma.refNum).note(note.id).delete().mustBeEmpty()

        val updatedNote = Notes.findOneById(note.id).run().futureValue.value
        updatedNote.deletedBy.value must === (1)

        withClue(updatedNote.deletedAt.value → Instant.now) {
          updatedNote.deletedAt.value.isBeforeNow mustBe true
        }

        // Deleted note should not be returned
        val allNotes = api(rma.refNum).get().as[Seq[AdminNotes.Root]]
        allNotes.map(_.id) must not contain note.id
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed with Order_Baked {
    val rma = Returns
      .create(Factories.rma.copy(orderRef = order.refNum, accountId = storeAdmin.accountId))
      .gimme
  }

}
