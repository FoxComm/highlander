package models.payment.storecredit

import java.time.Instant

import monocle.macros.GenLens
import org.json4s.JsonAST.JValue
import utils.db.ExPostgresDriver.api._
import utils.db._

case class StoreCreditCustom(id: Int = 0,
  adminId: Int,
  metadata: JValue,
  createdAt: Instant = Instant.now) extends FoxModel[StoreCreditCustom]

class StoreCreditCustoms(tag: Tag) extends FoxTable[StoreCreditCustom](tag, "store_credit_customs") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def metadata = column[JValue]("metadata")
  def createdAt = column[Instant]("created_at")

  def * = (id, adminId, metadata, createdAt) <> ((StoreCreditCustom.apply _).tupled, StoreCreditCustom.unapply)
}

object StoreCreditCustoms extends FoxTableQuery[StoreCreditCustom, StoreCreditCustoms](
  idLens = GenLens[StoreCreditCustom](_.id)
)(new StoreCreditCustoms(_)){
}
