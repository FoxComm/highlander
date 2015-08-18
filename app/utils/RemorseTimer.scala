package utils

import scala.concurrent.ExecutionContext
import akka.actor.Actor
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

import models.{Order, Orders}
import Order._

case object Tick

class RemorseTimer(implicit ec: ExecutionContext, db: Database) extends Actor {

  override def receive = {
    case Tick ⇒ tick
  }

  private def tick(implicit ec: ExecutionContext, db: Database) = {
    val orders = Orders
      .filter(_.status === (RemorseHold: Status))
      .filterNot(_.locked)

    for {
      advance ← db.run(orders
        .filterNot(_.remorsePeriodInMinutes > 0)
        .map(_.status)
        .update(Order.FulfillmentStarted))

      decrement ← db.stream(orders
        .filter(_.remorsePeriodInMinutes > 0)
        .mutate.transactionally)
        .foreach(mutate ⇒ mutate.row = mutate.row.copy(remorsePeriodInMinutes = mutate.row.remorsePeriodInMinutes - 1))
    } yield ()
  }
}
