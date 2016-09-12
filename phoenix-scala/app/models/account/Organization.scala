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

case class Organization(id: Int = 0,
                name: String,
                type: String,
                parentId: Int = 0,
                scopeId: Int = 0,
                createdAt: Instant = Instant.now,
                updatedAt: Instant = Instant.now,
                deletedAt: Option[Instant] = None)
    extends FoxModel[Organization]

class Organizations(tag: Tag) extends FoxTable[Organization](tag, "roles") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name      = column[String]("name")
  def type      = column[String]("type")
  def parentId  = column[Int]("parent_id")
  def scopeId   = column[Int]("scope_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, name, scopeId, createdAt, updatedAt, deletedAt) <> ((Organization.apply _).tupled, Organization.unapply)
}

object Organizations
  extends FoxTableQuery[Organization, Organizations](new Organizations(_)) 
  with ReturningId[Organization, Organizations] {

  val returningLens: Lens[Organization, Int] = lens[Organization].id

  def findByName(name: String): DBIO[Option[Organization]] = {
    filter(_.name === name).one
  }

  def findByNameInScope(name: String, scopeId: Int): DBIO[Option[Organization]] = {
    filter(_.name === name).filter(_.scopeId === scopeId).one
  }
}
