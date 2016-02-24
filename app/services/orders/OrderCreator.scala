package services.orders

import cats.implicits._
import models.product.ProductContext
import models.customer.{Customers, Customer}
import models.order.{Orders, Order}
import models.StoreAdmin
import payloads.CreateOrder
import responses.order.FullOrder
import FullOrder.Root
import responses.order.FullOrder
import services.{LogActivity, NotFoundFailure400, Result, ResultT}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._

import scala.concurrent.ExecutionContext
import models.activity.ActivityContext

object OrderCreator {
  def createCart(admin: StoreAdmin, payload: CreateOrder, productContext: ProductContext)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[Root] = {

    def existingCustomerOrNewGuest: Result[Root] = (payload.customerId, payload.email) match {
      case (Some(customerId), _)  ⇒ createCartForCustomer(customerId)
      case (_, Some(email))       ⇒ createCartAndGuest(email)
      case _                      ⇒ ???
    }

    def createCartForCustomer(customerId: Int): Result[Root] = (for {
        customer  ← * <~ Customers.mustFindById400(customerId)
        fullOrder ← * <~ OrderQueries.findOrCreateCartByCustomerInner(customer, productContext, Some(admin))
      } yield fullOrder).runTxn()

    def createCartAndGuest(email: String): Result[Root] = (for {
      guest ← * <~ Customers.create(Customer.buildGuest(email = email))
      cart  ← * <~ Orders.create(Order.buildCart(guest.id, productContext.id))
      _     ← * <~ LogActivity.cartCreated(Some(admin), root(cart, guest))
    } yield root(cart, guest)).runTxn()

    (for {
      _     ← ResultT.fromXor(payload.validate.toXor)
      root  ← ResultT(existingCustomerOrNewGuest)
    } yield root).value
  }

  private def root(order: Order, customer: Customer): Root = FullOrder.build(order = order, customer = customer.some)
}

