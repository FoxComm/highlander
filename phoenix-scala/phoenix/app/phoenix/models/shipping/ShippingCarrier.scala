package phoenix.models.shipping

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class ShippingCarrier(id: Int = 0,
                           name: String,
                           accountNumber: Option[String],
                           regionsServed: String = "US")
    extends FoxModel[ShippingCarrier]

class ShippingCarriers(tag: Tag) extends FoxTable[ShippingCarrier](tag, "shipping_methods") {
  def id            = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name          = column[String]("name")
  def accountNumber = column[Option[String]]("account_number")
  def regionsServed = column[String]("regions_served") // Should we enforce this?

  def * =
    (id, name, accountNumber, regionsServed) <> ((ShippingCarrier.apply _).tupled, ShippingCarrier.unapply)
}

object ShippingCarriers
    extends FoxTableQuery[ShippingCarrier, ShippingCarriers](new ShippingCarriers(_))
    with ReturningId[ShippingCarrier, ShippingCarriers] {
  val returningLens: Lens[ShippingCarrier, Int] = lens[ShippingCarrier].id
}
