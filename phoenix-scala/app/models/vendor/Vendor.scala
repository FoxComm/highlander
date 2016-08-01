package models.Vendor

import java.time.Instant
import models.location._
import slick.driver.PostgresDriver.api._
import utils.db._

case class Vendor(id: Int = 0,
                  name: Option[String],
                  isDisabled: Boolean = false,
                  createdAt: Instant = Instant.now)
    extends FoxModel[Vendor]
    with Validation[Vendor] {}


object Vendor {}

class Vendors(tag: Tag) extends FoxTable[Vendor](tag, "vendors") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[Option[String]]("name")
  def isDisabled = column[Boolean]("is_disabled")
  def createdAt = column[Instant]("created_at")

  def * = 
    (id,
     name,
     isDisabled
     createdAt) <> ((Vendor.apply _).tupled, Vendor.unapply)
}


