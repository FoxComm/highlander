package phoenix.models.cord

import java.time.Instant

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import core.failures.{Failures, GeneralFailure}
import phoenix.models.account._
import phoenix.models.cord.lineitems._
import phoenix.models.inventory.Skus
import phoenix.utils.aliases._
import phoenix.utils.time._
import phoenix.utils.{ADT, FSM}
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import core.utils.Money.Currency
import utils.db.ExPostgresDriver.api._
import utils.db._

case class Order(id: Int = 0,
                 scope: LTree,
                 referenceNumber: String = "",
                 accountId: Int,
                 currency: Currency = Currency.USD,
                 subTotal: Int = 0,
                 shippingTotal: Int = 0,
                 adjustmentsTotal: Int = 0,
                 taxesTotal: Int = 0,
                 grandTotal: Int = 0,
                 // Order-specific
                 contextId: Int,
                 state: Order.State = Order.RemorseHold,
                 // TODO: rename to `createdAt`
                 placedAt: Instant = Instant.now,
                 remorsePeriodEnd: Option[Instant] = Instant.now.plusMinutes(30).some,
                 fraudScore: Int = 0)
    extends CordBase[Order]
    with FSM[Order.State, Order] {

  def stateLens = lens[Order].state

  override def updateTo(newModel: Order): Either[Failures, Order] = super.transitionModel(newModel)

  override def primarySearchKey: String = referenceNumber

  import Order._

  val fsm: Map[State, Set[State]] = Map(
      FraudHold →
        Set(ManualHold, RemorseHold, FulfillmentStarted, Canceled),
      RemorseHold →
        Set(FraudHold, ManualHold, FulfillmentStarted, Canceled),
      ManualHold →
        Set(FraudHold, RemorseHold, FulfillmentStarted, Canceled),
      FulfillmentStarted →
        Set(Shipped, Canceled)
  )

  // If order is not in RemorseHold, remorsePeriodEnd should be None, but extra check wouldn't hurt
  val getRemorsePeriodEnd: Option[Instant] = state match {
    case RemorseHold ⇒ remorsePeriodEnd
    case _           ⇒ None
  }

  def getShippingState: Option[State] = Some(state)

  def mustBeRemorseHold: Either[Failures, Order] =
    if (state == RemorseHold) Either.right(this)
    else Either.left(GeneralFailure("Order is not in RemorseHold state").single)
}

object Order {
  sealed trait State extends Product with Serializable
  case object FraudHold          extends State
  case object RemorseHold        extends State
  case object ManualHold         extends State
  case object Canceled           extends State
  case object FulfillmentStarted extends State
  case object Shipped            extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

}

class Orders(tag: Tag) extends FoxTable[Order](tag, "orders") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope            = column[LTree]("scope")
  def referenceNumber  = column[String]("reference_number")
  def accountId        = column[Int]("account_id")
  def currency         = column[Currency]("currency")
  def subTotal         = column[Int]("sub_total")
  def shippingTotal    = column[Int]("shipping_total")
  def adjustmentsTotal = column[Int]("adjustments_total")
  def taxesTotal       = column[Int]("taxes_total")
  def grandTotal       = column[Int]("grand_total")
  def contextId        = column[Int]("context_id")
  def state            = column[Order.State]("state")
  def placedAt         = column[Instant]("placed_at")
  def remorsePeriodEnd = column[Option[Instant]]("remorse_period_end")
  def fraudScore       = column[Int]("fraud_score")

  def * =
    (id,
     scope,
     referenceNumber,
     accountId,
     currency,
     subTotal,
     shippingTotal,
     adjustmentsTotal,
     taxesTotal,
     grandTotal,
     contextId,
     state,
     placedAt,
     remorsePeriodEnd,
     fraudScore) <> ((Order.apply _).tupled, Order.unapply)
}

object Orders
    extends FoxTableQuery[Order, Orders](new Orders(_))
    with ReturningTableQuery[Order, Orders]
    with SearchByRefNum[Order, Orders] {

  def createFromCart(
      cart: Cart,
      subScope: Option[String])(implicit ec: EC, db: DB, ctx: OC, au: AU): DbResultT[Order] =
    createFromCart(cart, ctx.id, subScope)

  def createFromCart(cart: Cart,
                     contextId: Int,
                     subScope: Option[String])(implicit ec: EC, db: DB, au: AU): DbResultT[Order] =
    for {
      scope ← * <~ Scope.resolveOverride(subScope)

      //it is important that line items are grabbed before cart is created
      //because DB triggers delete the cart and line items after an order
      //is created.
      lineItems ← * <~ prepareOrderLineItemsFromCart(cart, contextId)

      order ← * <~ Orders.create(
                 Order(referenceNumber = cart.referenceNumber,
                       accountId = cart.accountId,
                       scope = scope,
                       currency = cart.currency,
                       subTotal = cart.subTotal,
                       shippingTotal = cart.shippingTotal,
                       adjustmentsTotal = cart.adjustmentsTotal,
                       taxesTotal = cart.taxesTotal,
                       grandTotal = cart.grandTotal,
                       contextId = contextId))

      _ ← * <~ OrderLineItems.createAll(lineItems)
    } yield order

  def prepareOrderLineItemsFromCart(cart: Cart, contextId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[OrderLineItem]] = {
    val uniqueSkuIdsInCart = CartLineItems.byCordRef(cart.referenceNumber).groupBy(_.skuId).map {
      case (skuId, q) ⇒ skuId
    }

    val skusInCart = for {
      skuId ← uniqueSkuIdsInCart
      sku   ← Skus if sku.id === skuId
    } yield (skuId, sku)

    for {
      skus      ← * <~ skusInCart.result
      skuMaps   ← * <~ skus.toMap
      lineItems ← * <~ CartLineItems.byCordRef(cart.referenceNumber).result
      orderLineItems ← * <~ lineItems.map { cli ⇒
                        val sku = skuMaps.get(cli.skuId).get
                        OrderLineItem(cordRef = cart.referenceNumber,
                                      referenceNumber = cli.referenceNumber,
                                      skuId = sku.id,
                                      skuShadowId = sku.shadowId,
                                      state = OrderLineItem.Pending,
                                      attributes = cli.attributes)
                      }
    } yield orderLineItems
  }

  def findByAccount(cust: Account): QuerySeq =
    findByAccountId(cust.id)

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def findByRefNum(refNum: String): QuerySeq =
    filter(_.referenceNumber === refNum)

  def findOneByRefNum(refNum: String): DBIO[Option[Order]] =
    filter(_.referenceNumber === refNum).one

  def findOneByRefNumAndAccount(refNum: String, account: Account): QuerySeq =
    filter(_.referenceNumber === refNum).filter(_.accountId === account.id)

  def findByRefNumAndAccountId(refNum: String, accountId: Int): QuerySeq =
    filter(_.referenceNumber === refNum).filter(_.accountId === accountId)

  type Ret       = (Int, String, Option[Instant])
  type PackedRet = (Rep[Int], Rep[String], Rep[Option[Instant]])
  private val rootLens = lens[Order]

  val returningLens: Lens[Order, (Int, String, Option[Instant])] = rootLens.id ~ rootLens.referenceNumber ~ rootLens.remorsePeriodEnd
  override val returningQuery = map { o ⇒
    (o.id, o.referenceNumber, o.remorsePeriodEnd)
  }
}
