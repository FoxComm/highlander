package services.notes

import models.Note
import models.account._
import utils.aliases._
import utils.db._

object CustomerNoteManager extends NoteManager[Int, User] {

  def noteType(): Note.ReferenceType = Note.Customer

  override def getEntityId(e: User): Int = e.accountId

  def fetchEntity(accountId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] =
    Users.mustFindByAccountId(accountId)
}
