import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.NotFoundFailure404
import models.{StoreAdmin, _}
import payloads.NotePayloads._
import responses.AdminNotes
import services.notes.StoreAdminNoteManager
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.time.RichInstant

class StoreAdminNotesIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/notes/store-admins/:adminId" - {
    "can be created by an admin for a customer" in new Fixture {
      val response =
        POST(s"v1/notes/store-admins/${storeAdmin.id}", CreateNote(body = "Hello, FoxCommerce!"))

      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must === ("Hello, FoxCommerce!")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/notes/store-admins/${storeAdmin.id}", CreateNote(body = ""))

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("body must not be empty")
    }

    "returns a 404 if the customer is not found" in new Fixture {
      val response = POST(s"v1/notes/store-admins/999999", CreateNote(body = ""))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 999999).description)
    }
  }

  "GET /v1/notes/store-admins/:adminId" - {

    "can be listed" in new Fixture {
      val createNotes = List("abc", "123", "xyz").map { body ⇒
        StoreAdminNoteManager.create(storeAdmin.id, storeAdmin, CreateNote(body = body))
      }
      DbResultT.sequence(createNotes).gimme

      val response = GET(s"v1/notes/store-admins/${storeAdmin.id}")
      response.status must === (StatusCodes.OK)

      val notes = response.as[Seq[AdminNotes.Root]]
      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }
  }

  "PATCH /v1/notes/store-admins/:adminId/:noteId" - {

    "can update the body text" in new Fixture {
      val rootNote = StoreAdminNoteManager
        .create(storeAdmin.id, storeAdmin, CreateNote(body = "Hello, FoxCommerce!"))
        .gimme

      val response = PATCH(s"v1/notes/store-admins/${storeAdmin.id}/${rootNote.id}",
                           UpdateNote(body = "donkey"))
      response.status must === (StatusCodes.OK)

      val note = response.as[AdminNotes.Root]
      note.body must === ("donkey")
    }
  }

  "DELETE /v1/notes/store-admins/:adminId/:noteId" - {

    "can soft delete note" in new Fixture {
      val createResp =
        POST(s"v1/notes/store-admins/${storeAdmin.id}", CreateNote(body = "Hello, FoxCommerce!"))
      val note = createResp.as[AdminNotes.Root]

      val response = DELETE(s"v1/notes/store-admins/${storeAdmin.id}/${note.id}")
      response.status must === (StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = Notes.findOneById(note.id).run().futureValue.value
      updatedNote.deletedBy.value === 1

      withClue(updatedNote.deletedAt.value → Instant.now) {
        updatedNote.deletedAt.value.isBeforeNow === true
      }

      // Deleted note should not be returned
      val allNotesResponse = GET(s"v1/notes/store-admins/${storeAdmin.id}")
      allNotesResponse.status must === (StatusCodes.OK)
      val allNotes = allNotesResponse.as[Seq[AdminNotes.Root]]
      allNotes.map(_.id) must not contain note.id

      val getDeletedNoteResponse = GET(s"v1/notes/store-admins/${storeAdmin.id}/${note.id}")
      getDeletedNoteResponse.status must === (StatusCodes.NotFound)
    }
  }

  trait Fixture extends StoreAdmin_Seed
}
