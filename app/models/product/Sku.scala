package models.product

import models.inventory.InventorySummaries
import scala.concurrent.ExecutionContext
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.time.JavaTimeSlickMapper._
import utils.{ADT, GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import com.pellucid.sealerate
import java.time.Instant
import monocle.macros.GenLens
import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}
import scala.concurrent.ExecutionContext
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import Aliases.Json

final case class Sku(
  id: Int = 0, 
  sku: String, 
  productId: Int,
  attributes: Json, 
  isHazardous: Boolean = false,
  isActive: Boolean = true,
  `type`: Sku.Type = Sku.Sellable)
  extends ModelWithIdParameter[Sku]

object Sku {
  sealed trait Type
  case object Sellable extends Type
  case object Preorder extends Type
  case object Backorder extends Type
  case object NonSellable extends Type
  object Type extends ADT[Type] { def types = sealerate.values[Type] }
  implicit val typeColumnType: JdbcType[Type] with BaseTypedType[Type] = Type.slickColumn
}

// This table mostly acts a placeholder in our system.  We may or may not import skus from 'origin' into this.
class Skus(tag: Tag) extends GenericTable.TableWithId[Sku](tag, "skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def sku = column[String]("sku")
  def productId = column[Int]("product_id")
  def attributes = column[Json]("attributes")
  def isHazardous = column[Boolean]("is_hazardous")
  def isActive = column[Boolean]("is_active")
  def `type` = column[Sku.Type]("type")

  def * = (id, sku, productId, attributes, isHazardous, isActive, `type`) <> ((Sku.apply _).tupled, Sku.unapply)

  def product = foreignKey(Products.tableName, productId, Products)(_.id)
}

object Skus extends TableQueryWithId[Sku, Skus](
  idLens = GenLens[Sku](_.id)
)(new Skus(_)) {

  def isAvailableOnHand(id: Int)(implicit ec: ExecutionContext, db: Database): Rep[Boolean] = {
    //TODO: Use inventory system here
    val HARD_CODED_WAREHOUSE_ID = 1
    InventorySummaries.findBySkuId(HARD_CODED_WAREHOUSE_ID, id).filter(s => (s.onHand - s.reserved) > 0).exists
  }

  def qtyAvailableForSkus(skus: Seq[String])(implicit ec: ExecutionContext, db: Database): DBIO[Map[Sku, Int]] = {
    //TODO: Use inventory system here
    (for {
      sku  ← Skus.filter(_.sku inSet skus)
      summ ← InventorySummaries if summ.skuId === sku.id
    } yield (sku, summ.onHand)).result.map(_.toMap)
  }
}
