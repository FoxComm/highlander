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
    baseMethods.map{ _.map { shippingMethod =>
      shippingMethod.shippingPriceRules.map{
        _.map { sRule =>
          // TODO: AW: Come back and deal with SKU-specific criteria later.
          sRule.orderCriteria.map {
            _.map { oCriterion =>
              oCriterion match {
                case t: OrderPriceCriterion =>
                  t.priceType match {
                    case t: OrderPriceCriterion.GrandTotal.type =>
                    case t: OrderPriceCriterion.SubTotal.type =>
                    case t: OrderPriceCriterion.GrandTotalLessShipping.type =>
                    case t: OrderPriceCriterion.GrandTotalLessTax.type =>
                  }
                case unknown => //could not find inherited objects or case classes
              }
            }
          }
        }

      }
      ShippingMethodWithPrice(displayName = "donkey", estimatedTime = "FOREVER", price = 3333)
    }
    }
  }
  // What is the price of a certain shipping method based on the current order details?

}
