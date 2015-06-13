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

case class LineItem(id: Int, parentId: Int, parentType: String, skuId: Int)

class LineItems(tag: Tag) extends Table[LineItem](tag, "line_items") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId = column[Int]("parent_id")
  def parentType = column[String]("parent_type")
  def skuId = column[Int]("sku_id")
  def * = (id, parentId, parentType, skuId) <> ((LineItem.apply _).tupled, LineItem.unapply)
}

object LineItems {
  val lineItems = TableQuery[LineItems]
  val returningId = lineItems.returning(lineItems.map(_.id))

  def findByCart(cart: Cart)(implicit db: Database) = {db.run(_findByCartId(cart.id).result) }

  def _findByCartId(cartId: Rep[Int]) = { lineItems.filter(_.parentId === cartId).filter(_.parentType === "cart") }
  def _findByOrderId(orderId: Rep[Int]) = { lineItems.filter(_.parentId === orderId).filter(_.parentType === "order") }
}
