package services.notes

import failures.NotFoundFailure404
import models.{Note, StoreAdmin, StoreAdmins}
import utils.aliases._
import utils.db._
import slick.driver.PostgresDriver.api._

object StoreAdminNoteManager extends NoteManager[Int, StoreAdmin] {

  def noteType(): Note.ReferenceType = Note.StoreAdmin

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdmin] =
    StoreAdmins.mustFindById404(id)

}
