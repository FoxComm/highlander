package models.Vendor

import java.time.Instant
import models.location._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.Validation
import shapeless._

case class Vendor(id: Int = 0,
                  name: Option[String],
                  description: Option[String],
                  taxId: Option[String],
                  createdAt: Instant = Instant.now)
    extends FoxModel[Vendor]
    with Validation[Vendor] {}

object Vendor {}

class Vendors(tag: Tag) extends FoxTable[Vendor](tag, "vendors") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name       = column[Option[String]]("name")
  def description = column[Option[String]]("description")
  def taxId      = columt[Option[String]]("tax_id")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, name, description, tax_id, createdAt) <> ((Vendor.apply _).tupled, Vendor.unapply)
}

object Vendors
    extends FoxTableQuery[Vendor, Vendors](new Vendors(_))
    with ReturningId[Vendor, Vendors] {

  val returningLens: Lens[Vendor, Int] = lens[Vendor].id

}
