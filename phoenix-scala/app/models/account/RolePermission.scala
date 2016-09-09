package models.account

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures._
import shapeless._
import utils.Validation
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class RolePermission(id: Int = 0, roleId: Int, permissionId: Int)
    extends FoxModel[RolePermission]

class RolePermissions(tag: Tag) extends FoxTable[RolePermission](tag, "role_permissions") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def roleId       = column[Int]("role_id")
  def permissionId = column[Int]("permission_id")

  def * =
    (id, roleId, permissionId) <> ((RolePermission.apply _).tupled, RolePermission.unapply)

  def role       = foreignKey(Roles.tableName, roleId, Roles)(_.id)
  def permission = foreignKey(Permissions.tableName, permissionId, Permissions)(_.id)
}

object RolePermissions
    extends FoxTableQuery[RolePermission, RolePermissions](new RolePermissions(_))
    with ReturningId[RolePermission, RolePermissions] {

  val returningLens: Lens[RolePermission, Int] = lens[RolePermission].id

  def findByRole(roleId: Int): DBIO[Option[RolePermission]] = {
    filter(_.roleId === roleId).one
  }

  def findByPermission(permissionId: Int): DBIO[Option[RolePermission]] = {
    filter(_.permissionId === permissionId).one
  }

}
