package services.activity

import models.Note
import responses.UserResponse

object NotesTailored {
  /* Notes */
  case class NoteCreated[T](admin: UserResponse.Root, entity: T, note: Note)
      extends ActivityBase[NoteCreated[T]]

  case class NoteUpdated[T](admin: UserResponse.Root, entity: T, oldNote: Note, note: Note)
      extends ActivityBase[NoteUpdated[T]]

  case class NoteDeleted[T](admin: UserResponse.Root, entity: T, note: Note)
      extends ActivityBase[NoteDeleted[T]]
}
