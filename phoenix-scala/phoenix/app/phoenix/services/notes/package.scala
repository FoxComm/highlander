package phoenix.services

import java.time.Instant

import core.db._
import core.failures.NotFoundFailure404
import phoenix.models.account._
import phoenix.models.{Note, Notes}
import phoenix.payloads.NotePayloads._
import phoenix.responses.AdminNoteResponse
import phoenix.responses.AdminNoteResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

package object notes {
  def forModel[M <: FoxModel[M]](finder: Notes.QuerySeq)(implicit ec: EC): DbResultT[Seq[AdminNoteResponse]] = {
    val query = for {
      notes   ← finder
      authors ← notes.author
    } yield (notes, authors)
    DbResultT.fromF(query.result.map(_.map {
      case (note, author) ⇒ AdminNoteResponse.build(note, author)
    }))
  }

  def createNote[T](entity: T, refId: Int, refType: Note.ReferenceType, author: User, payload: CreateNote)(
      implicit ec: EC,
      ac: AC,
      au: AU): DbResultT[Note] =
    for {
      note ← * <~ Notes.create(
              Note(storeAdminId = author.id,
                   referenceId = refId,
                   referenceType = refType,
                   body = payload.body,
                   scope = Scope.current))
      _ ← * <~ LogActivity().noteCreated(author, entity, note)
    } yield note

  def updateNote[T](entity: T, noteId: Int, author: User, payload: UpdateNote)(
      implicit ec: EC,
      ac: AC): DbResultT[AdminNoteResponse] =
    for {
      oldNote ← * <~ Notes
                 .filterByIdAndAdminId(noteId, author.id)
                 .mustFindOneOr(NotFoundFailure404(Note, noteId))
      newNote ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
      _       ← * <~ LogActivity().noteUpdated(author, entity, oldNote, newNote)
    } yield AdminNoteResponse.build(newNote, author)

  def deleteNote[T](entity: T, noteId: Int, admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      note ← * <~ Notes.mustFindById404(noteId)
      _    ← * <~ Notes.update(note, note.copy(deletedAt = Some(Instant.now), deletedBy = Some(admin.accountId)))
      _    ← * <~ LogActivity().noteDeleted(admin, entity, note)
    } yield ()
}
