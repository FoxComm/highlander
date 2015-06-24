package services

import models._

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._


object ShippingMethodsBuilder {
  case class ShippingMethodWithPrice(displayName: String, estimatedTime: String, price: Int)

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

    getAllTheShippingShit(order).map { (results: Seq[(ShippingMethod, ShippingMethodPriceRule, ShippingPriceRule, OrderPriceCriterion)]) ⇒
      results.map { case (method, methodRules, priceRule, criteria) ⇒
        // TODO: YAX/Ferdinand --> What's the appropriate way to handle a Future[Bool] below?
//        val shippingPrice = if (criteriaMatchForShippingRule(criteria, order)) {
//          priceRule.flatPrice
//        } else {
//          method.defaultPrice
//        }
        ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
      }
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
}
