package services

import java.time.Instant

import models.{Notes, StoreAdmin, Note}
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
  def forModel[M <: ModelWithIdParameter[M]](finder: Notes.QuerySeq)
    (implicit ec: EC, db: DB): DbResult[Seq[Root]] = {
    val query = for (notes ← finder; authors ← notes.author) yield (notes, authors)
    DbResult.fromDbio(query.result.map(_.map { case (note, author) ⇒ AdminNotes.build(note, author) }))
  }

  def createModelNote(refId: Int, refType: Note.ReferenceType, author: StoreAdmin,
    payload: payloads.CreateNote)(implicit ec: EC, db: DB): DbResultT[Root] = for {
    note ← * <~ Notes.create(Note(storeAdminId = author.id, referenceId = refId, referenceType = refType,
      body = payload.body))
  } yield AdminNotes.build(note, author)

  def updateNote(noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB): DbResultT[Root] = for {
    oldNote ← * <~ Notes.filterByIdAndAdminId(noteId, author.id).one.mustFindOr(NotFoundFailure404(Note, noteId))
    newNote ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
  } yield AdminNotes.build(newNote, author)

  def deleteNote(noteId: Int, admin: StoreAdmin)(implicit ec: EC, db: DB): Result[Unit] = (for {
    note ← * <~ Notes.mustFindById404(noteId)
    _    ← * <~ Notes.update(note, note.copy(deletedAt = Some(Instant.now), deletedBy = Some(admin.id)))
  } yield {}).runTxn()
}