package responses

import java.time.Instant
import models.{Note, StoreAdmin}

object AdminNotes {
  case class Root(id: Int, body: String, author: Author, createdAt: Instant) extends ResponseItem
  case class Author(name: String, email: String)                             extends ResponseItem

  def buildAuthor(author: StoreAdmin): Author =
    Author(name = author.name, email = author.email)

  def build(note: Note, author: StoreAdmin): Root =
    Root(id = note.id, body = note.body, author = buildAuthor(author), createdAt = note.createdAt)
}
