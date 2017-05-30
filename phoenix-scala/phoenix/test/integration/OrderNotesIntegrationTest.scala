import core.failures.NotFoundFailure404
import core.utils.time._
import phoenix.models._
import phoenix.models.cord._
import phoenix.payloads.NotePayloads._
import phoenix.responses.AdminNotes
import phoenix.responses.AdminNotes.Root
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

class OrderNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/notes/order/:refNum" - {
    "can be created by an admin for an order" in new Order_Baked {
      val note = notesApi.order(order.refNum).create(CreateNote("foo")).as[Root]
      note.body must === ("foo")
      note.author.name.value must === (defaultAdmin.name.value)
      note.author.email.value must === (defaultAdmin.email.value)
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
      val bodies = List("abc", "123", "xyz")

      bodies.foreach { body ⇒
        notesApi.order(order.refNum).create(CreateNote(body)).mustBeOk()
      }

      notesApi
        .order(order.refNum)
        .get()
        .as[Seq[Root]]
        .map(_.body) must contain theSameElementsAs bodies
    }
  }

  "PATCH /v1/notes/order/:refNum/:noteId" - {
    "can update the body text" in new Order_Baked {
      val note = notesApi.order(order.refNum).create(CreateNote("foo")).as[AdminNotes.Root]

      notesApi
        .order(order.refNum)
        .note(note.id)
        .update(UpdateNote("donkey"))
        .as[Root]
        .body must === ("donkey")
    }
  }

  "DELETE /v1/notes/order/:refNum/:noteId" - {
    "can soft delete note" in new Order_Baked {
      val note = notesApi.order(order.refNum).create(CreateNote("foo")).as[AdminNotes.Root]

      notesApi.order(order.refNum).note(note.id).delete().mustBeEmpty()

      val updatedNote = Notes.findOneById(note.id).gimme.value
      updatedNote.deletedBy.value must === (defaultAdmin.id)
      updatedNote.deletedAt.value.isBeforeNow mustBe true

      val allNotes = notesApi.order(order.refNum).get().as[Seq[Root]]
      allNotes.map(_.id) must not contain note.id
    }
  }
}
