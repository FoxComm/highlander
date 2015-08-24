package services

import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object ShippingManager {

  final case class ShippingData(order: models.Order, orderTotal: Int, orderSubTotal: Int,
    shippingAddress: models.OrderShippingAddress, shippingState: models.State)

  def evaluateStatement(order: models.Order, statement: models.QueryStatement)
    (implicit db: Database, ec: ExecutionContext): Future[Boolean] = {

    getShippingData(order).map {
      _ match {
        case Some(shippingData) ⇒
          evaluateStatementSync(shippingData, statement)
        case None ⇒
          // TODO (Jeff): We'll want real error handling here, not just false to be returned.
          false
      }
    }
  }

  private def evaluateStatementSync(shippingData: ShippingData, statement: models.QueryStatement): Boolean = {
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
        case models.QueryStatement.And ⇒ evaluateStatementSync(shippingData, nextCond) && result
        case models.QueryStatement.Or ⇒ evaluateStatementSync(shippingData, nextCond) || result
      }
    }
  }

  private def getShippingData(order: models.Order)
    (implicit db: Database, ec: ExecutionContext): Future[Option[ShippingData]] = {
    db.run(models.OrderShippingAddresses.findByOrderIdWithStates(order.id).result.headOption).flatMap { x ⇒
      val isNothing: Future[Option[ShippingData]] = Future.successful(None)

      x.fold(isNothing) { addressWithState ⇒

        order.grandTotal.flatMap { grandTotal ⇒
          order.subTotal.flatMap { subTotal ⇒
            val sd = ShippingData(order = order, orderTotal = grandTotal, orderSubTotal = subTotal,
              shippingAddress = addressWithState._1, shippingState = addressWithState._2)

            Future.successful(Some(sd))
          }
        }
      }
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
      case "stateId" ⇒
        models.Condition.matches(shippingData.shippingAddress.stateId, condition)
      case "stateName" ⇒
        models.Condition.matches(shippingData.shippingState.name, condition)
      case "stateAbbrev" ⇒
        models.Condition.matches(shippingData.shippingState.abbreviation, condition)
      case "zip" ⇒
        models.Condition.matches(shippingData.shippingAddress.zip, condition)
      case _ ⇒
        false
    }
  }

}