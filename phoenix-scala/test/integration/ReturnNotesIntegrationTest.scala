import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import failures.NotFoundFailure404
import models._
import models.returns._
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.ReturnNoteManager
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

  def api(ref: String) = notesApi.returns(ref)

  "Return Notes" - {
    "POST /v1/notes/return/:code" - {
      "can be created for return" in new Fixture {
        val note =
          api(rma.refNum).create(CreateNote(body = "Hello, FoxCommerce!")).as[AdminNotes.Root]

        note.body must === ("Hello, FoxCommerce!")
        note.author must === (AdminNotes.buildAuthor(storeAdmin))
      }

      "returns a validation error if failed to create" in new Fixture {
        val response = api(rma.refNum).create(CreateNote(body = ""))

        response.status must === (StatusCodes.BadRequest)
        response.error must === ("body must not be empty")
      }

      "returns a 404 if the return is not found" in new Fixture {
        private val none = "RMA-666"
        val response     = api(none).create(CreateNote(body = ""))

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, none).description)
      }
    }

    "GET /v1/notes/return/:code" - {

      "can be listed" in new Fixture {
        val createNotes = List("abc", "123", "xyz").map { body ⇒
          api(rma.refNum).create(CreateNote(body = body))
        }

        val response = api(rma.refNum).get()
        response.status must === (StatusCodes.OK)

        val notes = response.as[Seq[AdminNotes.Root]]
        notes must have size 3
        notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
      }
    }

    "PATCH /v1/notes/return/:code/:noteId" - {

      "can update the body text" in new Fixture {
        val rootNote =
          api(rma.refNum).create(CreateNote(body = "Hello, FoxCommerce!")).as[AdminNotes.Root]

        val note =
          api(rma.refNum).note(rootNote.id).update(UpdateNote(body = "donkey")).as[AdminNotes.Root]
        note.body must === ("donkey")
      }
    }

    "DELETE /v1/notes/return/:code/:noteId" - {

      "can soft delete note" in new Fixture {
        val note =
          api(rma.refNum).create(CreateNote(body = "Hello, FoxCommerce!")).as[AdminNotes.Root]

        val deleteResponse = api(rma.refNum).note(note.id).delete()
        deleteResponse.status must === (StatusCodes.NoContent)
        deleteResponse.bodyText mustBe empty

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
