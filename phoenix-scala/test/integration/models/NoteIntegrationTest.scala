package models

import util.CustomMatchers._
import util._
import util.fixtures.BakedFixtures
import utils.seeds.Seeds.Factories

class NoteIntegrationTest extends IntegrationTestBase with BakedFixtures with TestObjectContext {

  "Note" - {
    "Postgres constraints" - {
      "body is limited to 1000 characters" in new Fixture {
        val failure = Notes.create(note.copy(body = "z" * 1001)).run().futureValue.leftVal
        failure.getMessage must include("bodySize got 1001, expected 1000 or less")
      }

      "must have a body" in new Fixture {
        val failure = Notes.create(note.copy(body = "")).run().futureValue.leftVal
        failure.getMessage must include("body must not be empty")
      }
    }

    "validate" - {
      "fails when body is empty" in {
        val note   = Note(storeAdminId = 0, referenceId = 0, referenceType = Note.Order, body = "")
        val result = note.validate

        result must be('invalid)
        invalidValue(result) must includeFailure("body must not be empty")
      }

      "fails when body is more than 1000 characters" in {
        val note =
          Note(storeAdminId = 0, referenceId = 0, referenceType = Note.Order, body = "z" * 1001)
        val result = note.validate

        result must be('invalid)
        invalidValue(result) must includeFailure("bodySize got 1001, expected 1000 or less")
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val note = Factories.orderNotes.head.copy(storeAdminId = storeAdmin.accountId)
  }
}
