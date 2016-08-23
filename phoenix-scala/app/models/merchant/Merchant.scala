package models.Merchant

import java.time.Instant
import models.location._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.Validation
import shapeless._

case class Merchant(id: Int = 0,
                    name: Option[String],
                    description: Option[String],
                    taxId: Option[String],
                    isDisabled: Boolean = false,
                    createdAt: Instant = Instant.now)
    extends FoxModel[Merchant]
    with Validation[Merchant] {}

object Merchant {}

class Merchants(tag: Tag) extends FoxTable[Merchant](tag, "merchants") {
  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name        = column[Option[String]]("name")
  def description = column[Option[String]]("description")
  def taxId       = column[Option[String]]("tax_id")
  def isDisabled  = column[Boolean]("is_disabled")
  def createdAt   = column[Instant]("created_at")

  def * =
    (id, name, description, taxId, isDisabled, createdAt) <> ((Merchant.apply _).tupled, Merchant.unapply)
}

object Merchants
    extends FoxTableQuery[Merchant, Merchants](new Merchants(_))
    with ReturningId[Merchant, Merchants] {

  val returningLens: Lens[Merchant, Int] = lens[Merchant].id

}
