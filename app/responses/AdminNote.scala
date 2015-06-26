package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object AdminNote {

  type NoteResponse = Future[Option[Seq[Root]]]

  case class Root(id: Int, noteText: String, author: NoteAuthor)
  case class NoteAuthor(firstName: String, lastName: String)

  def build(note: OrderNote, author: StoreAdmin): Root = {
    Root(id = note.id, noteText = note.noteText, author = NoteAuthor(firstName = author.firstName, lastName = author.lastName))
  }

  def findNotesByOrderId(id: Int)
                        (implicit ec: ExecutionContext, db:Database): NoteResponse = {
    db.run(OrderNotes.filter(_.orderId === id).result).map { oNote ⇒
      Some(oNote.map { oN ⇒
        build(oN, oN.author)
      })
    }

  }
}

