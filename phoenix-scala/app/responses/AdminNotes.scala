package responses

import io.circe.syntax._
import java.time.Instant
import models.Note
import models.account.User
import utils.aliases.Json
import utils.json.codecs._

object AdminNotes {
  case class Root(id: Int, body: String, author: Author, createdAt: Instant) extends ResponseItem {
    def json: Json = this.asJson
  }
  case class Author(name: Option[String], email: Option[String]) extends ResponseItem {
    def json: Json = this.asJson
  }

  def buildAuthor(author: User): Author =
    Author(name = author.name, email = author.email)

  def build(note: Note, author: User): Root =
    Root(id = note.id, body = note.body, author = buildAuthor(author), createdAt = note.createdAt)
}
