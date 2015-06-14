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

case class CartLineItem(id: Int, cartId: Int, skuId: Int)

class CartLineItems(tag: Tag) extends Table[CartLineItem](tag, "cart_line_items") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cartId = column[Int]("cart_id")
  def skuId = column[Int]("sku_id")
  def * = (id, cartId, skuId) <> ((CartLineItem.apply _).tupled, CartLineItem.unapply)
}

object CartLineItems {
  val table = TableQuery[CartLineItems]
  val returningId = table.returning(table.map(_.id))

  def findByCart(cart: Cart)(implicit db: Database) = {db.run(_findByCartId(cart.id).result) }

  def _findByCartId(cartId: Rep[Int]) = { table.filter(_.cartId === cartId) }
  def _findByOrderId(orderId: Rep[Int]) = { table.filter(_.cartId === orderId) }
}
