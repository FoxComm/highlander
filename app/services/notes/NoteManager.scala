package services.notes

import java.time.Instant

import failures.NotFoundFailure404
import models.{Note, Notes, StoreAdmin}
import models.Notes.scope._
import responses.AdminNotes
import responses.AdminNotes.Root
import services._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

trait NoteManager[K, T <: FoxModel[T]] {
  // Define this methods in inherit object
  def noteType(): Note.ReferenceType
  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[T]

  // Use this methods wherever you want
  def list(key: K)(implicit ec: EC, db: DB, ac: AC): Result[Seq[Root]] = (for {
    entity   ← * <~ fetchEntity(key)
    response ← * <~ forModel(entityQuerySeq(entity.id))
  } yield response).run()

  def create(key: K, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB, ac: AC): Result[Root] = (for {
    entity ← * <~ fetchEntity(key)
    note   ← * <~ createInner(entity, entity.id, noteType(), author, payload)
  } yield AdminNotes.build(note, author)).runTxn()

  def update(key: K, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB, ac: AC): Result[Root] = (for {
    entity  ← * <~ fetchEntity(key)
    note    ← * <~ updateInner(entity, noteId, author, payload)
  } yield note).runTxn()

  def delete(key: K, noteId: Int, author: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {
    entity  ← * <~ fetchEntity(key)
    _       ← * <~ deleteInner(entity, noteId, author)
  } yield ()).runTxn()

  // Inner methods
  private def entityQuerySeq(entityId: Int)(implicit ec: EC, db: DB, ac: AC): Notes.QuerySeq =
    Notes.filter(_.referenceType === noteType()).filter(_.referenceId === entityId).notDeleted

  private def createInner(entity: T, refId: Int, refType: Note.ReferenceType, author: StoreAdmin,
    payload: payloads.CreateNote)(implicit ec: EC, db: DB, ac: AC): DbResultT[Note] = for {
    note   ← * <~ Notes.create(Note(storeAdminId = author.id, referenceId = refId, referenceType = refType,
      body = payload.body))
    _      ← * <~ LogActivity.noteCreated(author, entity, note)
  } yield note

  private def updateInner(entity: T, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB, ac: AC): DbResultT[Root] = for {
    oldNote ← * <~ Notes.filterByIdAndAdminId(noteId, author.id).one.mustFindOr(NotFoundFailure404(Note, noteId))
    newNote ← * <~ Notes.update(oldNote, oldNote.copy(body = payload.body))
    _       ← * <~ LogActivity.noteUpdated(author, entity, oldNote, newNote)
  } yield AdminNotes.build(newNote, author)

  private def deleteInner(entity: T, noteId: Int, admin: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] = for {
    note   ← * <~ Notes.mustFindById404(noteId)
    _      ← * <~ Notes.update(note, note.copy(deletedAt = Some(Instant.now), deletedBy = Some(admin.id)))
    _      ← * <~ LogActivity.noteDeleted(admin, entity, note)
  } yield ()

  private def forModel[M <: FoxModel[M]](finder: Notes.QuerySeq)
    (implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Root]] = {
    val query = for (notes ← finder; authors ← notes.author) yield (notes, authors)
    DbResult.fromDbio(query.result.map(_.map { case (note, author) ⇒ AdminNotes.build(note, author) }))
  }
}
