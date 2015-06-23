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
    baseMethods.flatMap { shippingMethods =>
      val blah = shippingMethods.map { shippingMethod =>

      ShippingPriceRules.shippingPriceRulesForShippingMethod(shippingMethod.id).flatMap { shippingRules =>
        val x = shippingRules.map { sRule =>
          // TODO: AW: Come back and deal with SKU-specific criteria later.
          ShippingPriceRulesOrderCriteria.criteriaForPricingRule(sRule.id).map { oCriteria =>
            val matches = oCriteria.filter { oCriterion =>
              criteriaMatchForShippingRule(oCriterion, order)
            }
            val shippingPrice = if (matches.nonEmpty) {
              sRule.flatPrice
            } else {
              shippingMethod.defaultPrice
            }
            ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = shippingPrice)
          }
        }

        Future.sequence(x)
      }
    }
      val accum: Future[Seq[ShippingMethodWithPrice]] = Future.successful(Seq())
      blah.foldLeft(accum) { case (ac, futureSeq) =>
        futureSeq.flatMap { seq =>
          ac.map { finalSeq =>
            finalSeq ++ seq
          }
        }
      }

      // This is where I want to assemble the actual price.

    }
  }
  // What is the price of a certain shipping method based on the current order details?


  def criteriaMatchForShippingRule(oCriterion: OrderPriceCriterion, order: Order): Boolean = {
    oCriterion match {
      case t: OrderPriceCriterion =>
        t.priceType match {
          case OrderPriceCriterion.GrandTotal =>
            val exactApplies = oCriterion.exactMatch.contains(order.grandTotal)
            val greaterApplies = oCriterion.greaterThan.exists(gThan => order.grandTotal >= gThan)
            val lessApplies = oCriterion.lessThan.exists(lThan => order.grandTotal >= lThan)
            (exactApplies || greaterApplies || lessApplies)
          case OrderPriceCriterion.SubTotal =>
            val exactApplies = oCriterion.exactMatch.contains(order.subTotal)
            val greaterApplies = oCriterion.greaterThan.exists(gThan => order.subTotal >= gThan)
            val lessApplies = oCriterion.lessThan.exists(lThan => order.subTotal >= lThan)
            (exactApplies || greaterApplies || lessApplies)
          case OrderPriceCriterion.GrandTotalLessShipping =>
            false
          case OrderPriceCriterion.GrandTotalLessTax =>
            false
          case _ =>
            false
        }
      case _ =>
        false //could not find inherited objects or case classes
    }
  }
}
