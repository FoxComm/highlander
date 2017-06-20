package phoenix.models.account

import java.time.Instant

import shapeless._
import slick.jdbc.PostgresProfile.api._
import core.db._

case class Organization(id: Int = 0,
                        name: String,
                        kind: String,
                        parentId: Option[Int],
                        scopeId: Int,
                        createdAt: Instant = Instant.now,
                        updatedAt: Instant = Instant.now,
                        deletedAt: Option[Instant] = None)
    extends FoxModel[Organization]

class Organizations(tag: Tag) extends FoxTable[Organization](tag, "organizations") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name      = column[String]("name")
  def kind      = column[String]("kind")
  def parentId  = column[Option[Int]]("parent_id")
  def scopeId   = column[Int]("scope_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, name, kind, parentId, scopeId, createdAt, updatedAt, deletedAt) <> ((Organization.apply _).tupled, Organization.unapply)
}

object Organizations
    extends FoxTableQuery[Organization, Organizations](new Organizations(_))
    with ReturningId[Organization, Organizations] {

  val returningLens: Lens[Organization, Int] = lens[Organization].id

  def findByName(name: String): DBIO[Option[Organization]] =
    filter(_.name === name).one

  def findByScopeId(scopeId: Int): DBIO[Option[Organization]] =
    filter(_.scopeId === scopeId).one

  def findByNameInScope(name: String, scopeId: Int): DBIO[Option[Organization]] =
    filter(_.name === name).filter(_.scopeId === scopeId).one

  def filterByIdAndScope(id: Int, scopeId: Int): QuerySeq =
    filter(_.id === id).filter(_.scopeId === scopeId)
}
