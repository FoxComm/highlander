package phoenix.models.account

import java.time.Instant

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class Role(id: Int = 0,
                name: String,
                scopeId: Int = 0,
                createdAt: Instant = Instant.now,
                updatedAt: Instant = Instant.now,
                deletedAt: Option[Instant] = None)
    extends FoxModel[Role]

class Roles(tag: Tag) extends FoxTable[Role](tag, "roles") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name      = column[String]("name")
  def scopeId   = column[Int]("scope_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, name, scopeId, createdAt, updatedAt, deletedAt) <> ((Role.apply _).tupled, Role.unapply)
}

object Roles extends FoxTableQuery[Role, Roles](new Roles(_)) with ReturningId[Role, Roles] {

  val returningLens: Lens[Role, Int] = lens[Role].id

  def findByName(name: String): DBIO[Option[Role]] =
    filter(_.name === name).one

  def findByNameInScope(name: String, scopeId: Int): DBIO[Option[Role]] =
    filter(_.name === name).filter(_.scopeId === scopeId).one

  def filterByScopeId(ids: Seq[Int], scopeId: Int): QuerySeq =
    filter(_.id.inSet(ids)).filter(_.scopeId === scopeId)
}
