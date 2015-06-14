package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object FullCart {
  type Response = Future[Option[Root]]

  case class Totals(subTotal: Int, taxes: Int, adjustments: Int, total: Int)
  case class Root(id: Int, lineItems: Seq[CartLineItem], adjustments: Seq[Adjustment], totals: Totals)

  def build(cart: Cart, lineItems: Seq[CartLineItem] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty): Root = {
    Root(id = cart.id, lineItems = lineItems, adjustments = adjustments, totals =
      Totals(subTotal = 500, taxes = 10, adjustments = 0, total = 510))
  }

  def findById(id: Int)
              (implicit ec: ExecutionContext,
               db: Database): Response = {

    val queries = for {
      cart <- Carts._findById(id)
      lineItems <- CartLineItems._findByCartId(cart.id)
    } yield (cart, lineItems)

    db.run(queries.result).map { results =>
      results.headOption.map { case (cart, _) =>
        build(cart, results.map { case (_, items) => items })
      }
    }
  }

  // Not sure if we want to unify/refactor this into one thing along with FindById.
  def findByCustomer(cust: Customer)
              (implicit ec: ExecutionContext,
               db: Database): Response = {

    val queries = for {
      cart <- Carts._findByCustomer(cust)
      lineItems <- CartLineItems._findByCartId(cart.id)
    } yield (cart, lineItems)

    db.run(queries.result).map { results =>
      results.headOption.map { case (cart, _) =>
        build(cart, results.map { case (_, items) => items })
      }
    }
  }

  def fromCart(cart: Cart)
              (implicit ec: ExecutionContext,
               db: Database): Response = {

    val queries = for {
      lineItems <- CartLineItems._findByCartId(cart.id)
    } yield lineItems

    db.run(queries.result).map { lineItems => Some(build(cart, lineItems)) }
  }
}
