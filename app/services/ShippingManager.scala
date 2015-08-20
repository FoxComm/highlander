package services

import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object ShippingManager {

  def evaluateStatement(order: models.Order, statement: models.ConditionStatement)
    (implicit db: Database, ec: ExecutionContext): Future[Boolean] = {

    val initial = statement.comparison match {
      case models.ConditionStatement.And ⇒ Future.successful(true)
      case models.ConditionStatement.Or ⇒ Future.successful(false)
    }

    val condResult = statement.conditions.foldLeft(initial) { (result, nextCond) ⇒
      result.flatMap { res ⇒
        val isMatch = nextCond.rootObject match {
          case "Order" ⇒ evaluateOrderCondition(order, nextCond)
          case "ShippingAddress" ⇒ evaluateShippingAddressCondition(order, nextCond)
          case _ ⇒ Future.successful(false)
        }

        isMatch.flatMap { im ⇒
          statement.comparison match {
            case models.ConditionStatement.And ⇒ Future.successful(im && res)
            case models.ConditionStatement.Or ⇒ Future.successful(im || res)
          }
        }
      }
    }

    statement.statements.foldLeft(condResult) { (result, nextStatement) ⇒
      result.flatMap { res ⇒
        evaluateStatement(order, nextStatement).flatMap { im ⇒
          statement.comparison match {
            case models.ConditionStatement.And ⇒ Future.successful(im && res)
            case models.ConditionStatement.Or ⇒ Future.successful(im || res)
          }
        }
      }
    }

  }

  private def evaluateOrderCondition(order: models.Order, condition: models.Condition)
    (implicit db: Database, ec: ExecutionContext): Future[Boolean] = {
    condition.field match {
      case "subtotal" ⇒
        order.subTotal.map { subTotal ⇒
          models.Condition.matches(subTotal, condition)
        }
      case "grandtotal" ⇒
        order.grandTotal.map { grandTotal ⇒
          models.Condition.matches(grandTotal, condition)
        }
      case _ ⇒
        Future.successful(false)
    }
  }

  private def evaluateShippingAddressCondition(order: models.Order, condition: models.Condition)
    (implicit db: Database, ec: ExecutionContext): Future[Boolean] = {

    db.run(models.OrderShippingAddresses.findByOrderIdWithStates(order.id).result.headOption).map { address ⇒
      address.fold(false) { a ⇒
        condition.field match {
          case "street1" ⇒
            models.Condition.matches(a._1.street1, condition)
          case "street2" ⇒
            models.Condition.matches(a._1.street2, condition)
          case "city" ⇒
            models.Condition.matches(a._1.city, condition)
          case "stateId" ⇒
            models.Condition.matches(a._1.stateId, condition)
          case "stateName" ⇒
            models.Condition.matches(a._2.name, condition)
          case "stateAbbrev" ⇒
            models.Condition.matches(a._2.abbreviation, condition)
          case "zip" ⇒
            models.Condition.matches(a._1.zip, condition)
          case _ ⇒
            false
        }
      }
    }

  }

}