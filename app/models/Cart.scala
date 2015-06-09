package models

import utils.{Validation, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class Cart(id: Int, accountId: Option[Int] = None) {

//  val lineItems: Seq[LineItem] = Seq.empty
//  val payments: Seq[Payment] = Seq.empty
//  val fulfillments: Seq[Fulfillment] = Seq.empty

  //  def coupons: Seq[Coupon] = Seq.empty
  //  def adjustments: Seq[Adjustment] = Seq.empty

  // TODO: how do we handle adjustment/coupon
  // specifically, promotions are handled at the checkout level, but need to display in the cart
//  def addCoupon(coupon: Coupon) = {}

  // carts support guest checkout
  def isGuest = this.accountId.isDefined

  // TODO: service class it?
}

class Carts(tag: Tag) extends Table[Cart](tag, "carts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Option[Int]]("account_id")
  def * = (id, accountId) <> ((Cart.apply _).tupled, Cart.unapply)
}

object Carts {
  val table = TableQuery[Carts]

  def findById(db: Database, id: Int): Future[Option[Cart]] = {
    db.run(table.filter(_.id === id).result.headOption)
  }
}
