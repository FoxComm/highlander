package services.notes

import failures.NotFoundFailure404
import models.Note
import models.account._
import utils.aliases._
import utils.db._
import slick.driver.PostgresDriver.api._

object StoreAdminNoteManager extends NoteManager[Int, User] {

  def noteType(): Note.ReferenceType = Note.StoreAdmin

  override def getEntityId(e: User): Int = e.accountId

  def fetchEntity(accountId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] =
    Users.mustFindByAccountId(accountId)

}
