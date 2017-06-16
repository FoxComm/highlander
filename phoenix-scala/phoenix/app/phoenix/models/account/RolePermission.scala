package phoenix.models.account

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class RolePermission(id: Int = 0, roleId: Int, permissionId: Int) extends FoxModel[RolePermission]

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

  def findByRoleId(roleId: Int): QuerySeq =
    filter(_.roleId === roleId)

  def findByRoles(roleIds: Seq[Int]): QuerySeq =
    filter(_.roleId.inSet(roleIds))

  def findByPermission(permissionId: Int): DBIO[Option[RolePermission]] =
    filter(_.permissionId === permissionId).one

}
