package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class NoteTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Note" - {
    "Postgres constraints" - {
      "body is limited to 1000 characters" in new Fixture {
        val failure = Notes.save(note.copy(body = "z" * 1001)).run().failed.futureValue
        failure.getMessage must include("value too long")
      }

      "must have a body" in new Fixture {
        val failure = Notes.save(note.copy(body = "")).run().failed.futureValue
        failure.getMessage must include( """violates check constraint "valid_body"""")
      }
    }

    "validate" - {
      "fails when body is empty" in {
        val note = Note(storeAdminId = 0, referenceId = 0, referenceType = Note.Order, body = "")
        val result = note.validate

        result must be ('invalid)
        result.messages.head must include ("body must not be empty")
      }

      "fails when body is more than 1000 characters" in {
        val note = Note(storeAdminId = 0, referenceId = 0, referenceType = Note.Order, body = "z" * 1001)
        val result = note.validate

        result must be ('invalid)
        result.messages.head must include ("expected 1000 or less")
      }
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val admin = (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }.run().futureValue
    val note = Factories.orderNotes.head.copy(storeAdminId = admin.id)
  }
}
