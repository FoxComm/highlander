package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object Note {

  final case class Root(id: Int, body: String, author: Author)
  final case class Author(firstName: String, lastName: String, email: String)

  def build(note: Note, author: StoreAdmin): Root = {
    Root(id = note.id, body = note.body,
      author = Author(firstName = author.firstName, lastName = author.lastName, email = author.email))
  }

  def findNotesByOrderId(id: Int)(implicit ec: ExecutionContext, db:Database): Future[Seq[Root]] = {
    (for {
      notes ← Notes._filterByOrderId(id)
      authors ← notes.author
    } yield (notes, authors)).result.run().map { results ⇒
      results.map { case (note, author) ⇒ build(note, author) }
    }
  }
}

