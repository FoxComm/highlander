package phoenix.services.notes

import core.db._
import phoenix.models.Note
import phoenix.models.account._
import phoenix.utils.aliases._

object CustomerNoteManager extends NoteManager[Int, User] {

  def noteType(): Note.ReferenceType = Note.Customer

  override def getEntityId(e: User): Int = e.accountId

  def fetchEntity(accountId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] =
    Users.mustFindByAccountId(accountId)
}
