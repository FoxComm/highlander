import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.NotFoundFailure404
import models.Notes
import models.customer.Customer
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.CustomerNoteManager
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.time.RichInstant

class CustomerNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/notes/customer/:customerId" - {
    "can be created by an admin for a customer" in new Fixture {
      val note = notesApi.customer(customer.id).create(CreateNote("foo")).as[AdminNotes.Root]
      note.body must === ("foo")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = notesApi.customer(customer.id).create(CreateNote(""))

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("body must not be empty")
    }

    "returns a 404 if the customer is not found" in new Fixture {
      val response = notesApi.customer(999999).create(CreateNote(""))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 999999).description)
    }
  }

  "GET /v1/notes/customer/:customerId" - {

    "can be listed" in new Fixture {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        CustomerNoteManager.create(customer.id, storeAdmin, CreateNote(body))
      }
      DbResultT.sequence(createNotes).gimme

      val notes = notesApi.customer(customer.id).get().as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/customer/:customerId/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote = CustomerNoteManager.create(customer.id, storeAdmin, CreateNote("foo")).gimme

      notesApi
        .customer(customer.id)
        .note(rootNote.id)
        .update(UpdateNote("donkey"))
        .as[AdminNotes.Root]
        .body must === ("donkey")
    }
  }

  "DELETE /v1/notes/customer/:customerId/:noteId" - {

    "can soft delete note" in new Fixture {
      val createResp = notesApi.customer(customer.id).create(CreateNote("foo"))
      val note       = createResp.as[AdminNotes.Root]

      val response = notesApi.customer(customer.id).note(note.id).delete()
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value must === (1)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow mustBe true
      }

      val allNotes = notesApi.customer(customer.id).get().as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse = notesApi.customer(customer.id).note(note.id).get()
      getDeletedNoteResponse.status must === (StatusCodes.NotFound)
    }
  }

  trait Fixture extends Customer_Seed with StoreAdmin_Seed
}
