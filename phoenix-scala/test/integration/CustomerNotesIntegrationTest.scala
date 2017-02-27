import java.time.Instant

import cats.implicits._
import failures.NotFoundFailure404
import models.Notes
import models.account._
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.CustomerNoteManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
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
      val note =
        notesApi.customer(customer.accountId).create(CreateNote("foo")).as[AdminNotes.Root]
      note.body must === ("foo")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      notesApi
        .customer(customer.accountId)
        .create(CreateNote(""))
        .mustFailWithMessage("body must not be empty")
    }

    "returns a 404 if the customer is not found" in new Fixture {
      notesApi
        .customer(999999)
        .create(CreateNote(""))
        .mustFailWith404(NotFoundFailure404(User, 999999))
    }
  }

  "GET /v1/notes/customer/:customerId" - {

    "can be listed" in new Fixture {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        CustomerNoteManager.create(customer.accountId, storeAdmin, CreateNote(body))
      }
      DbResultT.seqCollectFailures(createNotes).void.gimme

      val notes = notesApi.customer(customer.accountId).get().as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/customer/:customerId/:noteId" - {

    "can update the body text" in new Fixture {
      val note = CustomerNoteManager
        .create(customer.accountId, storeAdmin.copy(accountId = 1), CreateNote("foo"))
        .gimme

      notesApi
        .customer(customer.accountId)
        .note(note.id)
        .update(UpdateNote("donkey"))
        .as[AdminNotes.Root]
        .body must === ("donkey")
    }
  }

  "DELETE /v1/notes/customer/:customerId/:noteId" - {

    "can soft delete note" in new Fixture {
      val note =
        notesApi.customer(customer.accountId).create(CreateNote("foo")).as[AdminNotes.Root]

      notesApi.customer(customer.accountId).note(note.id).delete().mustBeEmpty()

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value must === (1)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow mustBe true
      }

      val allNotes = notesApi.customer(customer.accountId).get().as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id
    }
  }

  trait Fixture extends Customer_Seed with StoreAdmin_Seed
}
