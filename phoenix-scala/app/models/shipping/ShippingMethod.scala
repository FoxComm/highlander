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
                          name: String,
                          code: String,
                          carrier: Option[String] = None,
                          price: Int,
                          eta: Option[String] = None,
                          isActive: Boolean = true,
                          conditions: Option[QueryStatement] = None,
                          restrictions: Option[QueryStatement] = None)
    extends FoxModel[ShippingMethod] {

  def mustBeActive: Failures Xor ShippingMethod =
    if (isActive) Xor.right(this) else Xor.left(ShippingMethodIsNotActive(id).single)
}

object ShippingMethod {
  val standardShippingName  = "Standard shipping"
  val expressShippingName   = "2-3 day express"
  val overnightShippingName = "Overnight"

  val standardShippingCode     = "STANDARD"
  val standardShippingFreeCode = "STANDARD-FREE"
  val expressShippingCode      = "EXPRESS"
  val overnightShippingCode    = "OVERNIGHT"

  def buildFromCreatePayload(payload: CreateShippingMethodPayload): ShippingMethod =
    ShippingMethod(
        name = payload.name,
        code = payload.code,
        price = payload.price.value,
        eta = payload.eta,
        carrier = payload.carrier,
        isActive = true,
        conditions = payload.conditions,
        restrictions = payload.restrictions
    )

  def buildFromUpdatePayload(original: ShippingMethod,
                             payload: UpdateShippingMethodPayload): ShippingMethod =
    original.copy(
        parentId = Some(original.id),
        name = payload.name.getOrElse(original.name),
        price = payload.price.fold(original.price)(_.value),
        eta = payload.eta.fold(original.eta)(_ ⇒ payload.eta),
        carrier = payload.carrier.fold(original.carrier)(_ ⇒ payload.carrier),
        conditions = payload.conditions.fold(original.conditions)(_ ⇒ payload.conditions),
        restrictions = payload.restrictions.fold(original.restrictions)(_ ⇒ payload.restrictions)
    )
}

class ShippingMethods(tag: Tag) extends FoxTable[ShippingMethod](tag, "shipping_methods") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId     = column[Option[Int]]("parent_id")
  def name         = column[String]("name")
  def code         = column[String]("code")
  def carrier      = column[Option[String]]("carrier")
  def price        = column[Int]("price")
  def eta          = column[Option[String]]("eta")
  def isActive     = column[Boolean]("is_active")
  def conditions   = column[Option[QueryStatement]]("conditions")
  def restrictions = column[Option[QueryStatement]]("restrictions")

  def * =
    (id, parentId, name, code, carrier, price, eta, isActive, conditions, restrictions) <> ((ShippingMethod.apply _).tupled, ShippingMethod.unapply)
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
