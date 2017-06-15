package phoenix.services.activity

import phoenix.models.Note
import phoenix.responses.users.UserResponse

object NotesTailored {
  /* Notes */
  case class NoteCreated[T](admin: UserResponse, entity: T, note: Note) extends ActivityBase[NoteCreated[T]]

  case class NoteUpdated[T](admin: UserResponse, entity: T, oldNote: Note, note: Note)
      extends ActivityBase[NoteUpdated[T]]

  case class NoteDeleted[T](admin: UserResponse, entity: T, note: Note) extends ActivityBase[NoteDeleted[T]]
}
