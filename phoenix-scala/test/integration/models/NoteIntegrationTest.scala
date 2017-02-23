package models

import models.account.Scope
import testutils.CustomMatchers._
import testutils._
import testutils.fixtures.BakedFixtures
import utils.seeds.Seeds.Factories

class NoteIntegrationTest extends IntegrationTestBase with BakedFixtures with TestObjectContext {

  "Note" - {
    "Postgres constraints" - {
      "body is limited to 1000 characters" in new Fixture {
        val failure = Notes.create(note.copy(body = "z" * 1001)).gimmeFailures
        failure.getMessage must include("bodySize got 1001, expected 1000 or less")
      }

      "must have a body" in new Fixture {
        val failure = Notes.create(note.copy(body = "")).gimmeFailures
        failure.getMessage must include("body must not be empty")
      }
    }

    "validate" - {
      "fails when body is empty" in new StoreAdmin_Seed {
        val note = Note(storeAdminId = 0,
                        referenceId = 0,
                        referenceType = Note.Order,
                        body = "",
                        scope = Scope.current)
        val result = note.validate

        result must be('invalid)
        invalidValue(result) must includeFailure("body must not be empty")
      }

      "fails when body is more than 1000 characters" in new StoreAdmin_Seed {
        val note = Note(storeAdminId = 0,
                        referenceId = 0,
                        referenceType = Note.Order,
                        body = "z" * 1001,
                        scope = Scope.current)
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
