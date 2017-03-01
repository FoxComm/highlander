import cats.implicits._
import failures.NotFoundFailure404
import models._
import models.cord._
import payloads.NotePayloads._
import responses.AdminNotes
import responses.AdminNotes.Root
import services.notes.CordNoteManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
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
      val note = notesApi.order(order.refNum).create(CreateNote("foo")).as[Root]
      note.body must === ("foo")
      note.author must === (AdminNotes.buildAuthor(authedUser))
    }

    "returns a validation error if failed to create" in new Order_Baked {
      notesApi
        .order(order.refNum)
        .create(CreateNote(""))
        .mustFailWithMessage("body must not be empty")
    }

    "returns a 404 if the order is not found" in new Order_Baked {
      notesApi
        .order("NOPE")
        .create(CreateNote(""))
        .mustFailWith404(NotFoundFailure404(Cord, "NOPE"))
    }
  }

  "GET /v1/notes/order/:refNum" - {
    "can be listed" in new Order_Baked {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        CordNoteManager.create(order.refNum, storeAdmin, CreateNote(body))
      }
      DbResultT.seqCollectFailures(createNotes).gimme

      val notes = notesApi.order(order.refNum).get().as[Seq[Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/order/:refNum/:noteId" - {
    "can update the body text" in new Order_Baked {
      val rootNote = CordNoteManager.create(order.refNum, storeAdmin, CreateNote("foo")).gimme

      notesApi
        .order(order.refNum)
        .note(rootNote.id)
        .update(UpdateNote("donkey"))
        .as[Root]
        .body must === ("donkey")
    }
  }

  "DELETE /v1/notes/order/:refNum/:noteId" - {
    "can soft delete note" in new Order_Baked {
      val note = CordNoteManager.create(order.refNum, storeAdmin, CreateNote("foo")).gimme

      notesApi.order(order.refNum).note(note.id).delete().mustBeEmpty()

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value must === (1)
      updatedNote.deletedAt.value.isBeforeNow mustBe true

      val allNotes = notesApi.order(order.refNum).get().as[Seq[Root]]
      allNotes.map(_.id) must not contain note.id
    }
  }
}
