import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import models._
import models.cord._
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.OrderNoteManager
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.time._

class OrderNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/notes/order/:refNum" - {
    "can be created by an admin for an order" in new Order_Baked {
      val response = notesApi.order(order.refNum).create(CreateNote("Hello, FoxCommerce!"))
      response.status must === (StatusCodes.OK)
      val note = response.as[AdminNotes.Root]
      note.body must === ("Hello, FoxCommerce!")
      note.author must === (AdminNotes.buildAuthor(authedStoreAdmin))
    }

    "returns a validation error if failed to create" in new Order_Baked {
      val response = notesApi.order(order.refNum).create(CreateNote(""))
      response.status must === (StatusCodes.BadRequest)
      response.error must === ("body must not be empty")
    }

    "returns a 404 if the order is not found" in new Order_Baked {
      val response = notesApi.order("ABACADSF113").create(CreateNote(""))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "ABACADSF113").description)
    }
  }

  "GET /v1/notes/order/:refNum" - {
    "can be listed" in new Order_Baked {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        OrderNoteManager.create(order.refNum, storeAdmin, CreateNote(body))
      }
      DbResultT.sequence(createNotes).gimme

      val response = notesApi.order(order.refNum).get()
      response.status must === (StatusCodes.OK)
      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/order/:refNum/:noteId" - {
    "can update the body text" in new Order_Baked {
      val rootNote =
        OrderNoteManager.create(order.refNum, storeAdmin, CreateNote("Hello, FoxCommerce!")).gimme

      val response = notesApi.order(order.refNum).note(rootNote.id).update(UpdateNote("donkey"))
      response.status must === (StatusCodes.OK)
      val note = response.as[AdminNotes.Root]
      note.body must === ("donkey")
    }
  }

  "DELETE /v1/notes/order/:refNum/:noteId" - {
    "can soft delete note" in new Order_Baked {
      val note =
        OrderNoteManager.create(order.refNum, storeAdmin, CreateNote("Hello, FoxCommerce!")).gimme

      val response = notesApi.order(order.refNum).note(note.id).delete()
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value === 1
      updatedNote.deletedAt.value.isBeforeNow === true

      // Deleted note should not be returned
      val allNotesResponse = notesApi.order(order.refNum).get()
      allNotesResponse.status must === (StatusCodes.OK)
      val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse = notesApi.order(order.refNum).note(note.id).get()
      getDeletedNoteResponse.status must === (StatusCodes.NotFound)
    }
  }
}
