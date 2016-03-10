package services.notes

import models.Notes.scope._
import models.rma.Rmas
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import responses.AdminNotes
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object RmaNoteManager {

  def create(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    rma   ← * <~ Rmas.mustFindByRefNum(refNum)
    note  ← * <~ createModelNote(rma.id, Note.Rma, author, payload)
    _     ← * <~ LogActivity.noteCreated(author, rma, note)
  } yield AdminNotes.build(note, author)).runTxn()

  def update(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {
    rma  ← * <~ Rmas.mustFindByRefNum(refNum)
    note ← * <~ updateNote(rma, noteId, author, payload)
  } yield note).runTxn()

  def delete(refNum: String, noteId: Int, author: StoreAdmin)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] = (for {
    rma ← * <~ Rmas.mustFindByRefNum(refNum)
    _   ← * <~ deleteNote(rma, noteId, author)
  } yield ()).runTxn()

  def list(refNum: String)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    rma  ← * <~ Rmas.mustFindByRefNum(refNum)
    note ← * <~ forModel(Notes.filterByRmaId(rma.id).notDeleted)
  } yield note).run()
}
