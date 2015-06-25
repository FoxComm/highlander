package services

import models._

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._


object ShippingMethodsBuilder {
  case class ShippingMethodWithPrice(method: ShippingMethod, displayName: String, estimatedTime: String, price: Int)

  // Which shipping methods are active for this order?
  // 1) Do restriction check
  def availableShippingMethods(order: Order)
                              (implicit ec: ExecutionContext,
                               db: Database): Future[Seq[ShippingMethod]] = {
    val availMethods = ShippingMethods.filter(_.isActive).result
    db.run(availMethods).map(methods => methods)
  }

  def getAllTheShippingShit(order: Order)(implicit db: Database): Future[Seq[(ShippingMethod, ShippingMethodPriceRule, ShippingPriceRule, OrderPriceCriterion)]] = {
    val queries = for {
      methods ← ShippingMethods.filter(_.isActive)
      methodRules ← ShippingMethodsPriceRules.filter(_.shippingMethodId === methods.id)
      priceRules ← ShippingPriceRules.filter(_.id === methodRules.shippingPriceRuleId)
      criteriaMappings ← ShippingPriceRulesOrderCriteria.filter(_.shippingPricingRuleId === priceRules.id)
      criteria ← OrderPriceCriteria.filter(_.id === criteriaMappings.orderCriterionId)
    } yield (methods, methodRules, priceRules, criteria)

    db.run(queries.result)
  }

  def fullShippingMethodsForOrder(order: Order)
    (implicit ec: ExecutionContext, db: Database): Future[Seq[ShippingMethodWithPrice]] = {

    getAllTheShippingShit(order).flatMap { results ⇒
      val blah = results.map { case (method, methodRules, priceRule, criteria) ⇒
        // TODO: YAX/Ferdinand --> What's the appropriate way to handle a Future[Bool] below?
        criteriaMatchForShippingRule(criteria, order).map { matched ⇒
          val shippingPrice = if (matched) {
            priceRule.flatPrice
          } else {
            method.defaultPrice
          }

          ShippingMethodWithPrice(method = method, displayName = "donkey", estimatedTime = "FOREVER", price = shippingPrice)
        }
      }

      Future.sequence(blah)
    }
  }
  // What is the price of a certain shipping method based on the current order details?


  def criteriaMatchForShippingRule(oCriterion: OrderPriceCriterion, order: Order)
                                  (implicit ec: ExecutionContext, db: Database): Future[Boolean] = {
    oCriterion match {
      case t: OrderPriceCriterion =>
        t.priceType match {
          case OrderPriceCriterion.GrandTotal =>
            order.grandTotal.map { grandTotal ⇒
              val exactApplies = oCriterion.exactMatch.contains(grandTotal)
              val greaterApplies = oCriterion.greaterThan.exists(gThan => grandTotal >= gThan)
              val lessApplies = oCriterion.lessThan.exists(lThan => grandTotal <= lThan)

              (exactApplies || greaterApplies || lessApplies)
            }
          case OrderPriceCriterion.SubTotal =>
            order.subTotal.map { subTotal ⇒
              val exactApplies = oCriterion.exactMatch.contains(subTotal)
              val greaterApplies = oCriterion.greaterThan.exists(gThan => subTotal >= gThan)
              val lessApplies = oCriterion.lessThan.exists(lThan => subTotal <= lThan)

              (exactApplies || greaterApplies || lessApplies)
            }

/*          case OrderPriceCriterion.GrandTotalLessShipping =>
            false
          case OrderPriceCriterion.GrandTotalLessTax =>
            false*/
          case _ =>
            Future.successful(false)
        }
      case _ =>
        Future.successful(false)
//        false //could not find inherited objects or case classes
    }
  }

  def addShippingMethodToOrder(shippingMethodId: Int, order: Order)
                              (implicit ec: ExecutionContext, db: Database): Future[Order Or List[ErrorMessage]] = {
    val queries = for {
      shippingMethods <- ShippingMethods.findById(shippingMethodId)
      shipId <- shippingMethods.map { s => OrdersShippingMethods.insertOrUpdate(OrderShippingMethod(orderId = order.id, shippingMethodId = s.id)) }.getOrElse(DBIO.successful(0))
      updatedOrder <- Orders.findById(order.id) if shipId > 0
    } yield (updatedOrder)

    db.run(queries).map { optOrder =>
      optOrder.map(Good(_)).getOrElse((Bad(List("Shipping method was not saved"))))
    }
  }
}
