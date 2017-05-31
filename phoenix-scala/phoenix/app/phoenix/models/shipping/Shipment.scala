package phoenix.models.shipping

import java.time.Instant

import com.pellucid.sealerate
import core.db._
import phoenix.models.shipping.Shipment._
import phoenix.utils.ADT
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

case class Shipment(id: Int = 0,
                    cordRef: String,
                    orderShippingMethodId: Option[Int] = None,
                    shippingAddressId: Option[Int] = None,
                    state: Shipment.State = Cart,
                    shippingPrice: Option[Long] = None,
                    updatedAt: Option[Instant] = Some(Instant.now))
    extends FoxModel[Shipment]

object Shipment {
  sealed trait State
  case object Cart               extends State
  case object Ordered            extends State
  case object FraudHold          extends State //this only applies at the order_header level
  case object RemorseHold        extends State //this only applies at the order_header level
  case object ManualHold         extends State //this only applies at the order_header level
  case object Canceled           extends State
  case object FulfillmentStarted extends State
  case object PartiallyShipped   extends State
  case object Shipped            extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
}

class Shipments(tag: Tag) extends FoxTable[Shipment](tag, "shipments") {
  def id                    = column[Int]("id", O.PrimaryKey)
  def cordRef               = column[String]("cord_ref")
  def orderShippingMethodId = column[Option[Int]]("order_shipping_method_id")
  def shippingAddressId     = column[Option[Int]]("shipping_address_id") //Addresses table
  def state                 = column[Shipment.State]("state")
  def shippingPrice         = column[Option[Long]]("shipping_price") //gets filled in upon checkout
  def updatedAt             = column[Option[Instant]]("updated_at")

  def * =
    (id, cordRef, orderShippingMethodId, shippingAddressId, state, shippingPrice, updatedAt) <>
      ((Shipment.apply _).tupled, Shipment.unapply)
}

object Shipments
    extends FoxTableQuery[Shipment, Shipments](new Shipments(_))
    with ReturningId[Shipment, Shipments] {

  val returningLens: Lens[Shipment, Int] = lens[Shipment].id
}
