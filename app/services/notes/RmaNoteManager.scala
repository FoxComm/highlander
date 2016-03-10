package services.notes

import models.Notes.scope._
import models.rma.Rmas
import models.{Note, Notes, StoreAdmin}
import models.activity.ActivityContext
import responses.AdminNotes.Root
import services._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object RmaNoteManager {

  def createRmaNote(refNum: String, author: StoreAdmin, payload: payloads.CreateNote)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ createModelNote(rma.id, Note.Rma, author, payload)
  } yield response).runTxn()

  def updateRmaNote(refNum: String, noteId: Int, author: StoreAdmin, payload: payloads.UpdateNote)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    _        ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ updateNote(noteId, author, payload)
  } yield response).runTxn()

  def deleteRmaNote(noteId: Int, author: StoreAdmin)(implicit ec: EC, db: DB, ac: ActivityContext): Result[Unit] =
    deleteNote(noteId, author)

  def forRma(refNum: String)(implicit ec: EC, db: DB): Result[Seq[Root]] = (for {
    rma      ← * <~ Rmas.mustFindByRefNum(refNum)
    response ← * <~ forModel(Notes.filterByRmaId(rma.id).notDeleted)
  } yield response).run()
}
