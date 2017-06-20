import java.time.Instant

import core.failures.NotFoundFailure404
import phoenix.models.Notes
import phoenix.models.account._
import phoenix.payloads.NotePayloads._
import phoenix.responses.AdminNotes
import phoenix.utils.time.RichInstant
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

class CustomerNotesIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/notes/customer/:customerId" - {
    "can be created by an admin for a customer" in new Fixture {
      val note =
        notesApi.customer(customer.accountId).create(CreateNote("foo")).as[AdminNotes.Root]
      note.author.name.value must === (defaultAdmin.name.value)
      note.author.email.value must === (defaultAdmin.email.value)
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
      val bodies = List("abc", "123", "xyz")

      bodies.map { body ⇒
        notesApi.customer(customer.accountId).create(CreateNote(body)).mustBeOk()
      }

      notesApi
        .customer(customer.accountId)
        .get()
        .as[Seq[AdminNotes.Root]]
        .map(_.body)
        .toSet must contain theSameElementsAs bodies
    }
  }

  "PATCH /v1/notes/customer/:customerId/:noteId" - {

    "can update the body text" in new Fixture {
      val note =
        notesApi.customer(customer.accountId).create(CreateNote("foo")).as[AdminNotes.Root]

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

      val updatedNote = Notes.findOneById(note.id).gimme.value
      updatedNote.deletedBy.value must === (defaultAdmin.id)

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow mustBe true
      }

      val allNotes = notesApi.customer(customer.accountId).get().as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id
    }
  }

  trait Fixture extends Customer_Seed with StoreAdmin_Seed
}
