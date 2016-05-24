package services.activity

import models.Note
import responses.StoreAdminResponse

object NotesTailored {
  /* Notes */
  case class NoteCreated[T](admin: StoreAdminResponse.Root, entity: T, note: Note)
      extends ActivityBase[NoteCreated[T]]

  case class NoteUpdated[T](admin: StoreAdminResponse.Root, entity: T, oldNote: Note, note: Note)
      extends ActivityBase[NoteUpdated[T]]

  case class NoteDeleted[T](admin: StoreAdminResponse.Root, entity: T, note: Note)
      extends ActivityBase[NoteDeleted[T]]
}
