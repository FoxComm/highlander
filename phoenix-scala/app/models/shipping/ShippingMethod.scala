package models.shipping

import cats.data.Xor
import failures.Failures
import failures.ShippingMethodFailures.ShippingMethodIsNotActive
import models.cord.OrderShippingMethods
import models.rules.QueryStatement
import payloads.ShippingMethodPayloadsPayloads.{CreateShippingMethodPayload, UpdateShippingMethodPayload}
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ShippingMethod(id: Int = 0,
                          parentId: Option[Int] = None,
                          adminDisplayName: String,
                          storefrontDisplayName: String,
                          code: String,
                          shippingCarrierId: Option[Int] = None,
                          price: Int,
                          isActive: Boolean = true,
                          conditions: Option[QueryStatement] = None,
                          restrictions: Option[QueryStatement] = None)
    extends FoxModel[ShippingMethod] {

  def mustBeActive: Failures Xor ShippingMethod =
    if (isActive) Xor.right(this) else Xor.left(ShippingMethodIsNotActive(id).single)
}

object ShippingMethod {
  val standardShippingName          = "Standard shipping"
  val standardShippingNameForAdmin  = "Standard shipping (USPS)"
  val expressShippingName           = "2-3 day express"
  val expressShippingNameForAdmin   = "2-3 day express (FedEx)"
  val overnightShippingName         = "Overnight"
  val overnightShippingNameForAdmin = "Overnight (FedEx)"

  val standardShippingCode     = "STANDARD"
  val standardShippingFreeCode = "STANDARD-FREE"
  val expressShippingCode      = "EXPRESS"
  val overnightShippingCode    = "OVERNIGHT"

  def buildFromCreatePayload(payload: CreateShippingMethodPayload): ShippingMethod =
    ShippingMethod(
        adminDisplayName = payload.adminDisplayName,
        storefrontDisplayName = payload.storefrontDisplayName,
        code = payload.code,
        price = payload.price,
        isActive = true
    )

  def buildFromUpdatePayload(original: ShippingMethod,
                             payload: UpdateShippingMethodPayload): ShippingMethod =
    original.copy(
        parentId = Some(original.id),
        adminDisplayName = payload.adminDisplayName.getOrElse(original.adminDisplayName),
        storefrontDisplayName =
          payload.storefrontDisplayName.getOrElse(original.storefrontDisplayName),
        price = payload.price.getOrElse(original.price)
    )
}

class ShippingMethods(tag: Tag) extends FoxTable[ShippingMethod](tag, "shipping_methods") {
  def id                    = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId              = column[Option[Int]]("parent_id")
  def adminDisplayName      = column[String]("admin_display_name")
  def storefrontDisplayName = column[String]("storefront_display_name")
  def code                  = column[String]("code")
  def shippingCarrierId     = column[Option[Int]]("shipping_carrier_id")
  def price                 = column[Int]("price")
  def isActive              = column[Boolean]("is_active")
  def conditions            = column[Option[QueryStatement]]("conditions")
  def restrictions          = column[Option[QueryStatement]]("restrictions")

  def * =
    (id,
     parentId,
     adminDisplayName,
     storefrontDisplayName,
     code,
     shippingCarrierId,
     price,
     isActive,
     conditions,
     restrictions) <> ((ShippingMethod.apply _).tupled, ShippingMethod.unapply)
}

object ShippingMethods
    extends FoxTableQuery[ShippingMethod, ShippingMethods](new ShippingMethods(_))
    with ReturningId[ShippingMethod, ShippingMethods] {

  val returningLens: Lens[ShippingMethod, Int] = lens[ShippingMethod].id

  def findActive: Query[ShippingMethods, ShippingMethod, Seq] = filter(_.isActive === true)

  def findActiveById(id: Int): QuerySeq = findActive.filter(_.id === id)

  def forCordRef(refNum: String): QuerySeq =
    for {
      orderShippingMethod ← OrderShippingMethods.filter(_.cordRef === refNum)
      shipMethod          ← ShippingMethods.filter(_.id === orderShippingMethod.shippingMethodId)
    } yield shipMethod
}
