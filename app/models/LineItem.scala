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

case class LineItem(id: Int, cartId: Int, skuId: Int)

class LineItems(tag: Tag) extends Table[LineItem](tag, "line_items") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cartId = column[Int]("cart_id")
  def skuId = column[Int]("sku_id")
  def * = (id, cartId, skuId) <> ((LineItem.apply _).tupled, LineItem.unapply)
}

