package models.account

import java.time.Instant
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class Permission(id: Int = 0,
                      scopeId: Int,
                      resourceId: Int,
                      frn: String,
                      actions: List[String],
                      createdAt: Instant = Instant.now,
                      updatedAt: Instant = Instant.now,
                      deletedAt: Option[Instant] = None)
    extends FoxModel[Permission]

class Permissions(tag: Tag) extends FoxTable[Permission](tag, "permissions") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scopeId    = column[Int]("scope_id")
  def resourceId = column[Int]("resource_id")
  def frn        = column[String]("frn")
  def actions    = column[List[String]]("actions")
  def createdAt  = column[Instant]("created_at")
  def updatedAt  = column[Instant]("updated_at")
  def deletedAt  = column[Option[Instant]]("deleted_at")

  def * =
    (id, scopeId, resourceId, frn, actions, createdAt, updatedAt, deletedAt) <> ((Permission.apply _).tupled, Permission.unapply)
}

object Permissions
    extends FoxTableQuery[Permission, Permissions](new Permissions(_))
    with ReturningId[Permission, Permissions] {

  val returningLens: Lens[Permission, Int] = lens[Permission].id

}
