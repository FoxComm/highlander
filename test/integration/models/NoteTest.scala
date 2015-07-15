package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import util.IntegrationTestBase
import utils.Seeds.Factories

class NoteTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Note" - {
    "is limited to 1000 characters" in new Fixture {
      val failure = Notes.save(note.copy(body = "z" * 1001)).run().failed.futureValue
      failure.getMessage must include ("value too long")
    }

    "must have a body" in new Fixture {
      val failure = Notes.save(note.copy(body = "")).run().failed.futureValue
      failure.getMessage must include ("""violates check constraint "valid_body"""")
    }
  }

  trait Fixture {
    val adminFactory = Factories.storeAdmin
    val admin = (StoreAdmins.returningId += adminFactory).map { id ⇒ adminFactory.copy(id = id) }.run().futureValue
    val note = Factories.orderNotes.head.copy(storeAdminId = admin.id)
  }
}
