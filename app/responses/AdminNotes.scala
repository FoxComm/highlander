package responses

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.{GiftCard, Note, Order, StoreAdmin, Customer}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.ModelWithIdParameter
import models.Notes
import models.Notes.scope._

object AdminNotes {
  final case class Root(id: Int, body: String, author: Author, createdAt: Instant) extends ResponseItem
  final case class Author(firstName: String, lastName: String, email: String) extends ResponseItem

  def buildAuthor(author: StoreAdmin): Author =
    Author(firstName = author.firstName, lastName = author.lastName, email = author.email)

  def build(note: Note, author: StoreAdmin): Root =
    Root(id = note.id, body = note.body, author = buildAuthor(author), createdAt = note.createdAt)

  def forOrder(order: Order)(implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] =
    forModel(Notes.filterByOrderId(order.id).notDeleted)

  def forGiftCard(giftCard: GiftCard)(implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] =
    forModel(Notes.filterByGiftCardId(giftCard.id).notDeleted)

  def forCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] =
    forModel(Notes.filterByCustomerId(customer.id).notDeleted)

  private def forModel[M <: ModelWithIdParameter](finder: Query[Notes, Note, Seq])
    (implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] = {
    (for {
      notes ← finder
      authors ← notes.author
    } yield (notes, authors)).result.run().map { results ⇒
      results.map { case (note, author) ⇒ build(note, author) }
    }.map(Xor.right)
  }
}

