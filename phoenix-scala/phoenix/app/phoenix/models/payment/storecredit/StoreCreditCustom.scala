package phoenix.models.payment.storecredit

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import phoenix.utils.aliases._
import shapeless._

case class StoreCreditCustom(id: Int = 0, adminId: Int, metadata: Json, createdAt: Instant = Instant.now)
    extends FoxModel[StoreCreditCustom]

class StoreCreditCustoms(tag: Tag) extends FoxTable[StoreCreditCustom](tag, "store_credit_customs") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId   = column[Int]("admin_id")
  def metadata  = column[Json]("metadata")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, adminId, metadata, createdAt) <> ((StoreCreditCustom.apply _).tupled, StoreCreditCustom.unapply)
}

object StoreCreditCustoms
    extends FoxTableQuery[StoreCreditCustom, StoreCreditCustoms](new StoreCreditCustoms(_))
    with ReturningId[StoreCreditCustom, StoreCreditCustoms] {
  val returningLens: Lens[StoreCreditCustom, Int] = lens[StoreCreditCustom].id
}
