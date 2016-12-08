package services

import java.time.Instant

import failures.NotFoundFailure404
import models.{Note, Notes}
import models.account._
import payloads.NotePayloads._
import responses.AdminNotes
import responses.AdminNotes.Root
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

package object notes {
  def forModel[M <: FoxModel[M]](finder: Notes.QuerySeq)(implicit ec: EC): DbResultT[Seq[Root]] = {
    val query = for (notes ← finder; authors ← notes.author) yield (notes, authors)
    DbResultT.fromDbio(query.result.map(_.map {
      case (note, author) ⇒ AdminNotes.build(note, author)
    }))
  }

  def createNote[T](entity: T,
                    refId: Int,
                    refType: Note.ReferenceType,
                    author: User,
                    payload: CreateNote)(implicit ec: EC, ac: AC, au: AU): DbResultT[Note] =
    for {
      note ← * <~ Notes.create(
                Note(storeAdminId = author.id,
                     referenceId = refId,
                     referenceType = refType,
                     body = payload.body,
                     scope = Scope.current))
      _ ← * <~ LogActivity.noteCreated(author, entity, note)
    } yield note

  def updateNote[T](entity: T, noteId: Int, author: User, payload: UpdateNote)(
      implicit ec: EC,
      ac: AC): DbResultT[Root] =
    for {
      oldNote ← * <~ Notes
                 .filterByIdAndAdminId(noteId, author.id)
                 .mustFindOneOr(NotFoundFailure404(Note, noteId))
      newNote ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
      _       ← * <~ LogActivity.noteUpdated(author, entity, oldNote, newNote)
    } yield AdminNotes.build(newNote, author)

  def deleteNote[T](entity: T, noteId: Int, admin: User)(implicit ec: EC,
                                                         db: DB,
                                                         ac: AC): DbResultT[Unit] =
    for {
      note ← * <~ Notes.mustFindById404(noteId)
      _ ← * <~ Notes.update(
             note,
             note.copy(deletedAt = Some(Instant.now), deletedBy = Some(admin.accountId)))
      _ ← * <~ LogActivity.noteDeleted(admin, entity, note)
    } yield {}
}
