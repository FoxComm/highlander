package services

import models.{ShippingMethods, ShippingMethod}
import models.rules.{Condition, QueryStatement}
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import Result._

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  final case class ShippingData(order: models.Order, orderTotal: Int, orderSubTotal: Int,
    shippingAddress: models.OrderShippingAddress, shippingRegion: models.Region)

  def getShippingMethodsForOrder(order: models.Order)(implicit db: Database, ec: ExecutionContext):
    Result[Seq[responses.ShippingMethods.Root]] = {

    val queries = for {
      orderShippingAddresses ← models.OrderShippingAddresses.findByOrderIdWithRegions(order.id).result.headOption
      subTotal ← OrderTotaler._subTotalForOrder(order)
      grandTotal ← OrderTotaler._grandTotalForOrder(order)
      shippingMethods ← ShippingMethods.findActive.result
    } yield (orderShippingAddresses, subTotal, grandTotal, shippingMethods)

    db.run(queries).flatMap {
      case (Some((address, region)), subTotal, grandTotal, shippingMethods) ⇒

        val shippingData = ShippingData(order, grandTotal.getOrElse(0), subTotal.getOrElse(0), address, region)

        val matchingMethods = shippingMethods.filter { shippingMethod ⇒
          shippingMethod.conditions.fold(false) { condition ⇒
            val statement = condition.extract[QueryStatement]
            evaluateStatement(shippingData, statement)
          }
        }

        val methodResponses = matchingMethods.map { method ⇒
          val enabled = method.conditions.fold(true) { condition ⇒
            val statement = condition.extract[QueryStatement]
            evaluateStatement(shippingData, statement)
          }
          responses.ShippingMethods.build(method, enabled)
        }

        right(methodResponses)

      case (None, _, _, _) ⇒
        left(OrderShippingMethodsCannotBeProcessed(order.refNum).single)
    }
  }

  private def evaluateStatement(shippingData: ShippingData, statement: QueryStatement): Boolean = {
    val initial = statement.comparison == QueryStatement.And

    val conditionsResult = statement.conditions.foldLeft(initial) { (result, nextCond) ⇒
      val res = nextCond.rootObject match {
        case "Order" ⇒ evaluateOrderCondition(shippingData, nextCond)
        case "ShippingAddress" ⇒ evaluateShippingAddressCondition(shippingData, nextCond)
        case _ ⇒ false
      }

      statement.comparison match {
        case QueryStatement.And ⇒ result && res
        case QueryStatement.Or ⇒ result || res
      }
    }

    statement.statements.foldLeft(conditionsResult) { (result, nextCond) ⇒
      statement.comparison match {
        case QueryStatement.And ⇒ evaluateStatement(shippingData, nextCond) && result
        case QueryStatement.Or ⇒ evaluateStatement(shippingData, nextCond) || result
      }
    }
  }

  private def evaluateOrderCondition(shippingData: ShippingData, condition: Condition): Boolean = {
    condition.field match {
      case "subtotal" ⇒ Condition.matches(shippingData.orderSubTotal, condition)
      case "grandtotal" ⇒ Condition.matches(shippingData.orderTotal, condition)
      case _ ⇒ false
    }
  }

  private def evaluateShippingAddressCondition(shippingData: ShippingData, condition: Condition): Boolean = {
    condition.field match {
      case "address1" ⇒
        Condition.matches(shippingData.shippingAddress.address1, condition)
      case "address2" ⇒
        Condition.matches(shippingData.shippingAddress.address2, condition)
      case "city" ⇒
        Condition.matches(shippingData.shippingAddress.city, condition)
      case "regionId" ⇒
        Condition.matches(shippingData.shippingAddress.regionId, condition)
      case "regionName" ⇒
        Condition.matches(shippingData.shippingRegion.name, condition)
      case "regionAbbrev" ⇒
        Condition.matches(shippingData.shippingRegion.abbreviation, condition)
      case "zip" ⇒
        Condition.matches(shippingData.shippingAddress.zip, condition)
      case _ ⇒
        false
    }
  }

}
