package phoenix.responses

import java.time.Instant

import phoenix.models.Note
import phoenix.models.account.User

object AdminNotes {
  case class Root(id: Int, body: String, author: Author, createdAt: Instant) extends ResponseItem
  case class Author(name: Option[String], email: Option[String])             extends ResponseItem

  def buildAuthor(author: User): Author =
    Author(name = author.name, email = author.email)

  def build(note: Note, author: User): Root =
    Root(id = note.id, body = note.body, author = buildAuthor(author), createdAt = note.createdAt)
}
