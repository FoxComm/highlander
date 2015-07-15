package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object AdminNote {

  type Response = Future[Option[Seq[Root]]]

  final case class Root(id: Int, noteText: String, author: Author)
  final case class Author(firstName: String, lastName: String, email: String)

  def build(note: Note, author: StoreAdmin): Root = {
    Root(id = note.id, noteText = note.body, author = Author(firstName = author.firstName, lastName = author.lastName, email = author.email))
  }

  def findNotesByOrderId(id: Int)
                        (implicit ec: ExecutionContext, db:Database): Response = {

    Future.successful(None)
//    val query = for {
//      orderNotes ← Notes.filter(_.orderId === id)
//      noteAuthors ← orderNotes.author
//    } yield (orderNotes, noteAuthors)
//
//    db.run(query.result).map { results ⇒
//      Some(results.map {
//        case (orderNote, noteAuthor) ⇒
//          build(orderNote, noteAuthor)
//      })
//    }

  }
}

