package responses

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.{Note, Notes, Order, StoreAdmin}
import slick.driver.PostgresDriver.api._

object AdminNotes {
  final case class Root(id: Int, body: String, author: Author)
  final case class Author(firstName: String, lastName: String, email: String)

  def buildAuthor(author: StoreAdmin): Author =
    Author(firstName = author.firstName, lastName = author.lastName, email = author.email)

  def build(note: Note, author: StoreAdmin): Root =
    Root(id = note.id, body = note.body, author = buildAuthor(author))

  def forOrder(order: Order)(implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] = {
    (for {
      notes   ← Notes._filterByOrderId(order.id)
      authors ← notes.author
    } yield (notes, authors)).result.run().map { results ⇒
      results.map { case (note, author) ⇒ build(note, author) }
    }.map(Xor.right)
  }
}

