import cats.implicits._
import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import failures.NotFoundFailure404
import models._
import models.returns._
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.ReturnNoteManager
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time.RichInstant

class ReturnNotesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "Return Notes" - {
    pending

    "POST /v1/notes/return/:code" - {
      "can be created by an admin for a gift card" in new Fixture {
        val response =
          POST(s"v1/notes/rma/${rma.refNum}", CreateNote(body = "Hello, FoxCommerce!"))

        response.status must === (StatusCodes.OK)

        val note = response.as[AdminNotes.Root]
        note.body must === ("Hello, FoxCommerce!")
        note.author must === (AdminNotes.buildAuthor(storeAdmin))
      }

      "returns a validation error if failed to create" in new Fixture {
        val response = POST(s"v1/notes/rma/${rma.refNum}", CreateNote(body = ""))

        response.status must === (StatusCodes.BadRequest)
        response.error must === ("body must not be empty")
      }

      "returns a 404 if the gift card is not found" in new Fixture {
        val response = POST(s"v1/notes/rma/RMA-666", CreateNote(body = ""))

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "RMA-666").description)
      }
    }

    "GET /v1/notes/return/:code" - {

      "can be listed" in new Fixture {
        val createNotes = List("abc", "123", "xyz").map { body ⇒
          ReturnNoteManager.create(rma.refNum, storeAdmin, CreateNote(body = body))
        }
        DbResultT.seqCollectFailures(createNotes).gimme

        val response = GET(s"v1/notes/rma/${rma.refNum}")
        response.status must === (StatusCodes.OK)

        val notes = response.as[Seq[AdminNotes.Root]]
        notes must have size 3
        notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
      }
    }

    "PATCH /v1/notes/return/:code/:noteId" - {

      "can update the body text" in new Fixture {
        val rootNote = ReturnNoteManager
          .create(rma.refNum, storeAdmin, CreateNote(body = "Hello, FoxCommerce!"))
          .gimme

        val response =
          PATCH(s"v1/notes/rma/${rma.refNum}/${rootNote.id}", UpdateNote(body = "donkey"))
        response.status must === (StatusCodes.OK)

        val note = response.as[AdminNotes.Root]
        note.body must === ("donkey")
      }
    }

    "DELETE /v1/notes/return/:code/:noteId" - {

      "can soft delete note" in new Fixture {
        val createResp =
          POST(s"v1/notes/rma/${rma.refNum}", CreateNote(body = "Hello, FoxCommerce!"))
        val note = createResp.as[AdminNotes.Root]

        val response = DELETE(s"v1/notes/rma/${rma.refNum}/${note.id}")
        response.status must === (StatusCodes.NoContent)
        response.bodyText mustBe empty

        val updatedNote = Notes.findOneById(note.id).run().futureValue.value
        updatedNote.deletedBy.value must === (1)

        withClue(updatedNote.deletedAt.value → Instant.now) {
          updatedNote.deletedAt.value.isBeforeNow mustBe true
        }

        // Deleted note should not be returned
        val allNotesResponse = GET(s"v1/notes/rma/${rma.refNum}")
        allNotesResponse.status must === (StatusCodes.OK)
        val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
        allNotes.map(_.id) must not contain note.id

        val getDeletedNoteResponse = GET(s"v1/notes/rma/${rma.refNum}/${note.id}")
        getDeletedNoteResponse.status must === (StatusCodes.NotFound)
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed with Order_Baked {
    val rma = Returns
      .create(Factories.rma.copy(orderRef = order.refNum, accountId = customer.accountId))
      .gimme
  }
}
