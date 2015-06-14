package models

import utils.{Validation, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


case class Order(id: Int, customerId: Int, status: Order.Status, locked: Int) extends LineItemable {
  def lineItemParentId = this.id
}

object Order {
  sealed trait Status
  object Status {
    case object New extends Status
    case object FraudHold extends Status
    case object RemorseHold extends Status
    case object ManualHold extends Status
    case object Canceled extends Status
    case object FulfillmentStarted extends Status
    case object PartiallyShipped extends Status
    case object Shipped extends Status
  }

  implicit val StatusColumnType = MappedColumnType.base[Status, String](
    { stat =>
        stat match {
          case t: Status.New.type => "new"
          case t: Status.FraudHold.type => "fraudhold"
          case t: Status.RemorseHold.type => "remorsehold"
          case t: Status.ManualHold.type => "manualhold"
          case t: Status.Canceled.type => "canceled"
          case t: Status.FulfillmentStarted.type => "fulfillmentstarted"
          case t: Status.PartiallyShipped.type => "partiallyshipped"
          case t: Status.Shipped.type => "shipped"
          case _ => "wtf"
        }
    },
    { str =>
        str match {
          case "new" => Status.New
          case "fraudhold" => Status.FraudHold
          case "remorsehold" => Status.RemorseHold
          case "manualhold" => Status.ManualHold
          case "canceled" => Status.Canceled
          case "fulfillmentstarted" => Status.FulfillmentStarted
          case "partiallyshipped" => Status.PartiallyShipped
          case "shipped" => Status.Shipped
          case _ => Status.New
        }
    }
  )
}

class Orders(tag: Tag) extends Table[Order](tag, "orders") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customer_id = column[Int]("customer_id")
  def status = column[Order.Status]("status")
  def locked = column[Int]("locked") // 0 for no; 1 for yes
  def * = (id, customer_id, status, locked) <> ((Order.apply _).tupled, Order.unapply)
}

object Orders {
  val table = TableQuery[Orders]
  val returningId = table.returning(table.map(_.id))

  def _create(order: Order)(implicit ec: ExecutionContext, db: Database): DBIOAction[models.Order, NoStream, Effect.Write] = {
   for { newId <- Orders.returningId += order
   } yield ( order.copy(id = newId) )
  }
}