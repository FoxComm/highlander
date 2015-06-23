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


  def fullShippingMethodsForOrder(order: Order)
                                 (implicit ec: ExecutionContext,
                                   db: Database): Future[Seq[ShippingMethodWithPrice]] = {
    val baseMethods = availableShippingMethods(order)
    baseMethods.flatMap{ _.map { shippingMethod =>
      ShippingPriceRules.shippingPriceRulesForShippingMethod(shippingMethod.id).map{
        _.map { sRule =>
          // TODO: AW: Come back and deal with SKU-specific criteria later.
          ShippingPriceRulesOrderCriteria.criteriaForPricingRule(sRule.id).map {
            _.map { oCriterion =>
              oCriterion match {
                case t: OrderPriceCriterion =>
                  t.priceType match {
                    case OrderPriceCriterion.GrandTotal =>
                      val exactApplies = oCriterion.exactMatch.contains(order.grandTotal)
                      val greaterApplies = oCriterion.greaterThan.exists(gThan => order.grandTotal >= gThan)
                      val lessApplies = oCriterion.lessThan.exists(lThan => order.grandTotal >= lThan)
                      if (exactApplies || greaterApplies || lessApplies) {
                        ShippingMethodWithPrice(displayName = shippingMethod.storefrontDisplayName, estimatedTime = "Long Time", price = sRule.flatPrice)
                      } else {
                        ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
                      }
                    case OrderPriceCriterion.SubTotal =>
                      val exactApplies = oCriterion.exactMatch.contains(order.subTotal)
                      val greaterApplies = oCriterion.greaterThan.exists(gThan => order.subTotal >= gThan)
                      val lessApplies = oCriterion.lessThan.exists(lThan => order.subTotal >= lThan)
                      if (exactApplies || greaterApplies || lessApplies) {
                        ShippingMethodWithPrice(displayName = shippingMethod.storefrontDisplayName, estimatedTime = "Long Time", price = sRule.flatPrice)
                      } else {
                        ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
                      }
                    case OrderPriceCriterion.GrandTotalLessShipping =>
                      ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
                    case OrderPriceCriterion.GrandTotalLessTax =>
                      ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
                    case _ => ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
                  }
                case _ =>
                  ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)//could not find inherited objects or case classes
              }
            }
          }
        }

      }
      // This is where I want to assemble the actual price.
      //ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
    }
    }
  }
  // What is the price of a certain shipping method based on the current order details?

}
