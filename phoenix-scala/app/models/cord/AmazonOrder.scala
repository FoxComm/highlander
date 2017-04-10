package models.cord

import java.time.Instant
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.db._

case class AmazonOrder(id: Int = 0,
                       amazonOrderId: String = "",
                       orderTotal: Int = 0,
                       paymentMethodDetail: String = "",
                       orderType: String = "",
                       currency: Currency = Currency.USD,
                       orderStatus: String = "",
                       createdAt: Instant = Instant.now,
                       updatedAt: Instant = Instant.now,
                       archivedAt: Option[Instant] = None)
    extends FoxModel[AmazonOrder]

object AmazonOrder {
//  val namePattern = "[^@]+"
}

class AmazonOrders(tag: Tag) extends FoxTable[AmazonOrder](tag, "amazon_orders") {
  def id                  = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def amazonOrderId       = column[String]("amazon_order_id")
  def orderTotal          = column[Int]("order_total")
  def paymentMethodDetail = column[String]("payment_method_detail")
  def orderType           = column[String]("order_type")
  def currency            = column[Currency]("currency")
  def orderStatus         = column[String]("order_status")
  def createdAt           = column[Instant]("created_at")
  def updatedAt           = column[Instant]("updated_at")
  def archivedAt          = column[Instant]("archived_at")

  def * =
    (id,
     amazonOrderId,
     orderTotal,
     paymentMethodDetail,
     orderType,
     currency,
     orderStatus,
     createdAt,
     updatedAt,
     archivedAt) <> ((AmazonOrder.apply _).tupled, AmazonOrder.unapply)
}

object AmazonOrders
    extends FoxTableQuery[AmazonOrder, AmazonOrders](new AmazonOrders(_))
    with ReturningId[AmazonOrder, AmazonOrders]
    with SearchByAmazonOrderId[AmazonOrder, AmazonOrders] {

  val returningLens: Lens[AmazonOrder, Int] = lens[AmazonOrder].id
}
