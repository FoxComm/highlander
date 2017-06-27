package phoenix.responses

import java.time.Instant

import phoenix.models.Note
import phoenix.models.account.User

case class NoteAuthorResponse(name: Option[String], email: Option[String]) extends ResponseItem

object NoteAuthorResponse {

  def build(author: User): NoteAuthorResponse =
    NoteAuthorResponse(name = author.name, email = author.email)
}

case class AdminNoteResponse(id: Int, body: String, author: NoteAuthorResponse, createdAt: Instant)
    extends ResponseItem

object AdminNoteResponse {

  def build(note: Note, author: User): AdminNoteResponse =
    AdminNoteResponse(id = note.id,
                      body = note.body,
                      author = NoteAuthorResponse.build(author),
                      createdAt = note.createdAt)
}
