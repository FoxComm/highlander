import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import models.customer.{Customer, Customers}
import models.{Notes, _}
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.CustomerNoteManager
import util._
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time.RichInstant

class CustomerNotesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC {

  "POST /v1/notes/customer/:customerId" - {
    "can be created by an admin for a customer" in new Fixture {
      val response =
        POST(s"v1/notes/customer/${customer.id}", CreateNote(body = "Hello, FoxCommerce!"))

      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("Hello, FoxCommerce!")
      note.author must ===(AdminNotes.buildAuthor(admin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/notes/customer/${customer.id}", CreateNote(body = ""))

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===("body must not be empty")
    }

    "returns a 404 if the customer is not found" in new Fixture {
      val response = POST(s"v1/notes/customer/999999", CreateNote(body = ""))

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Customer, 999999).description)
    }
  }

  "GET /v1/notes/customer/:customerId" - {

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        CustomerNoteManager.create(customer.id, admin, CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/notes/customer/${customer.id}")
      response.status must ===(StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must ===(Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/customer/:customerId/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote = CustomerNoteManager
        .create(customer.id, admin, CreateNote(body = "Hello, FoxCommerce!"))
        .gimme

      val response =
        PATCH(s"v1/notes/customer/${customer.id}/${rootNote.id}", UpdateNote(body = "donkey"))
      response.status must ===(StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must ===("donkey")
    }
  }

  "DELETE /v1/notes/customer/:customerId/:noteId" - {

    "can soft delete note" in new Fixture {
      val createResp =
        POST(s"v1/notes/customer/${customer.id}", CreateNote(body = "Hello, FoxCommerce!"))
      val note = createResp.as[AdminNotes.Root]

      val response = DELETE(s"v1/notes/customer/${customer.id}/${note.id}")
      response.status must ===(StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value === 1

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow === true
      }

      // Deleted note should not be returned
      val allNotesResponse = GET(s"v1/notes/customer/${customer.id}")
      allNotesResponse.status must ===(StatusCodes.OK)
      val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse = GET(s"v1/notes/customer/${customer.id}/${note.id}")
      getDeletedNoteResponse.status must ===(StatusCodes.NotFound)
    }
  }

  trait Fixture {
    val (admin, customer) = (for {
      admin    ← * <~ StoreAdmins.create(authedStoreAdmin)
      customer ← * <~ Customers.create(Factories.customer)
    } yield (admin, customer)).gimme
  }
}
