package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import util.IntegrationTestBase
import utils.Seeds.Factories

class NoteTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Note" - {
    "is limited to 1000 characters" in {
      val failure = Notes.save(Factories.orderNotes.head.copy(text = "z" * 1001)).run().failed.futureValue
      failure.getMessage must include ("value too long")
    }
  }
}
