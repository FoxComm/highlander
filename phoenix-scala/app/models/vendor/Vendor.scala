package models.Vendor

import java.time.Instant
import models.location._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.Validation
import shapeless._
import com.pellucid.sealerate

case class Vendor(id: Int = 0,
                  name: Option[String],
                  description: Option[String],
                  state: Vendor.State = Vendor.New,
                  createdAt: Instant = Instant.now)
    extends FoxModel[Vendor]
    with Validation[Vendor] {}

object Vendor {
  sealed trait State

  case object New       extends State
  case object Active    extends State
  case object Suspended extends State
  case object Dropped   extends State
  case object Inactive  extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

}

class Vendors(tag: Tag) extends FoxTable[Vendor](tag, "vendors") {
  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name        = column[Option[String]]("name")
  def description = column[Option[String]]("description")
  def state       = column[Vendor.State]("state")
  def createdAt   = column[Instant]("created_at")

  def * =
    (id, name, description, state, createdAt) <> ((Vendor.apply _).tupled, Vendor.unapply)
}

object Vendors
    extends FoxTableQuery[Vendor, Vendors](new Vendors(_))
    with ReturningId[Vendor, Vendors] {

  val returningLens: Lens[Vendor, Int] = lens[Vendor].id

}
