package services

import java.time.Instant

import failures.NotFoundFailure404
import models.{Note, Notes, StoreAdmin}
import responses.AdminNotes
import responses.AdminNotes.Root
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.ModelWithIdParameter
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

package object notes {
  def forModel[M <: ModelWithIdParameter[M]](finder: Notes.QuerySeq)(implicit ec: EC): DbResult[Seq[Root]] = {
    val query = for (notes ← finder; authors ← notes.author) yield (notes, authors)
    DbResult.fromDbio(query.result.map(_.map { case (note, author) ⇒ AdminNotes.build(note, author) }))
  }

  def createNote[T](entity: T, refId: Int, refType: Note.ReferenceType, author: StoreAdmin,
    payload: payloads.CreateNote)(implicit ec: EC, ac: AC): DbResultT[Note] = for {
    note ← * <~ Notes.create(Note(storeAdminId = author.id, referenceId = refId, referenceType = refType,
      body = payload.body))
    _    ← * <~ LogActivity.noteCreated(author, entity, note)
  } yield note

  def updateNote[T](entity: T, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, ac: AC): DbResultT[Root] = for {
    oldNote ← * <~ Notes.filterByIdAndAdminId(noteId, author.id).one.mustFindOr(NotFoundFailure404(Note, noteId))
    newNote ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
    _       ← * <~ LogActivity.noteUpdated(author, entity, oldNote, newNote)
  } yield AdminNotes.build(newNote, author)

  def deleteNote[T](entity: T, noteId: Int, admin: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {
    note ← * <~ Notes.mustFindById404(noteId)
    _    ← * <~ Notes.update(note, note.copy(deletedAt = Some(Instant.now), deletedBy = Some(admin.id)))
    _    ← * <~ LogActivity.noteDeleted(admin, entity, note)
  } yield {}).runTxn()
}
