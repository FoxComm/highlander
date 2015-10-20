package responses

import java.time.Instant
import models.{Note, StoreAdmin}



object AdminNotes {
  final case class Root(id: Int, body: String, author: Author, createdAt: Instant) extends ResponseItem
  final case class Author(firstName: String, lastName: String, email: String) extends ResponseItem

  def buildAuthor(author: StoreAdmin): Author =
    Author(firstName = author.firstName, lastName = author.lastName, email = author.email)

  def build(note: Note, author: StoreAdmin): Root =
    Root(id = note.id, body = note.body, author = buildAuthor(author), createdAt = note.createdAt)
}

