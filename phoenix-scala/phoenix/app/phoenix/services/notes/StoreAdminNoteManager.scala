package phoenix.services.notes

import core.failures.NotFoundFailure404
import phoenix.models.Note
import phoenix.models.account._
import phoenix.utils.aliases._
import core.db._
import slick.jdbc.PostgresProfile.api._

object StoreAdminNoteManager extends NoteManager[Int, User] {

  def noteType(): Note.ReferenceType = Note.StoreAdmin

  override def getEntityId(e: User): Int = e.accountId

  def fetchEntity(accountId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] =
    Users.mustFindByAccountId(accountId)

}
