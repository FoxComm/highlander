package services

import models.{ShippingMethods, ShippingMethod}
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.JsonFormatters
import Result._

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  final case class ShippingData(order: models.Order, orderTotal: Int, orderSubTotal: Int,
    shippingAddress: models.OrderShippingAddress, shippingRegion: models.Region)

  def getShippingMethodsForOrder(order: models.Order)(implicit db: Database, ec: ExecutionContext):
    Result[Seq[ShippingMethod]] = {

    getShippingData(order).flatMap {
      case Some(shippingData) ⇒
        db.run(ShippingMethods.findActive.result).flatMap { shippingMethods ⇒
          val matchingMethods = shippingMethods.filter { shippingMethod ⇒
            val statement = shippingMethod.conditions.extract[models.QueryStatement]
            evaluateStatement(shippingData, statement)
          }

          right(matchingMethods)
        }
      case None ⇒
        left(OrderShippingMethodsCannotBeProcessed(order.refNum))
    }
  }

  private def evaluateStatement(shippingData: ShippingData, statement: models.QueryStatement): Boolean = {
    val initial = statement.comparison == models.QueryStatement.And

    val conditionsResult = statement.conditions.foldLeft(initial) { (result, nextCond) ⇒
      val res = nextCond.rootObject match {
        case "Order" ⇒ evaluateOrderCondition(shippingData, nextCond)
        case "ShippingAddress" ⇒ evaluateShippingAddressCondition(shippingData, nextCond)
        case _ ⇒ false
      }

      statement.comparison match {
        case models.QueryStatement.And ⇒ result && res
        case models.QueryStatement.Or ⇒ result || res
      }
    }

    statement.statements.foldLeft(conditionsResult) { (result, nextCond) ⇒
      statement.comparison match {
        case models.QueryStatement.And ⇒ evaluateStatement(shippingData, nextCond) && result
        case models.QueryStatement.Or ⇒ evaluateStatement(shippingData, nextCond) || result
      }
    }
  }

  private def getShippingData(order: models.Order)
    (implicit db: Database, ec: ExecutionContext): Future[Option[ShippingData]] = {
    val actions = for {
      orderShippingAddresses ← models.OrderShippingAddresses.findByOrderIdWithRegions(order.id).result.headOption
      subTotal ← OrderTotaler._subTotalForOrder(order)
      grandTotal ← OrderTotaler._grandTotalForOrder(order)
    } yield (orderShippingAddresses, subTotal, grandTotal)

    db.run(actions).map {
      case (Some(addressWithRegion), subTotal, grandTotal) ⇒
        Some(ShippingData(order, grandTotal.getOrElse(0), subTotal.getOrElse(0),
          addressWithRegion._1, addressWithRegion._2))

      case (None, _, _) ⇒
        None
    }
  }

  private def evaluateOrderCondition(shippingData: ShippingData, condition: models.Condition): Boolean = {
    condition.field match {
      case "subtotal" ⇒ models.Condition.matches(shippingData.orderSubTotal, condition)
      case "grandtotal" ⇒ models.Condition.matches(shippingData.orderTotal, condition)
      case _ ⇒ false
    }
  }

  private def evaluateShippingAddressCondition(shippingData: ShippingData, condition: models.Condition): Boolean = {
    condition.field match {
      case "street1" ⇒
        models.Condition.matches(shippingData.shippingAddress.street1, condition)
      case "street2" ⇒
        models.Condition.matches(shippingData.shippingAddress.street2, condition)
      case "city" ⇒
        models.Condition.matches(shippingData.shippingAddress.city, condition)
      case "regionId" ⇒
        models.Condition.matches(shippingData.shippingAddress.regionId, condition)
      case "regionName" ⇒
        models.Condition.matches(shippingData.shippingRegion.name, condition)
      case "regionAbbrev" ⇒
        models.Condition.matches(shippingData.shippingRegion.abbreviation, condition)
      case "zip" ⇒
        models.Condition.matches(shippingData.shippingAddress.zip, condition)
      case _ ⇒
        false
    }
  }

}