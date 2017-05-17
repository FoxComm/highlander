package phoenix.payloads

object NotePayloads {

  case class CreateNote(body: String)

  case class UpdateNote(body: String)
}
