package models.inventory

import models.Aliases.Json
import models.product.Products
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.Slick.implicits._
import utils.table.SearchByCode
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class Sku(id: Int = 0, code: String, productId: Int, attributes: Json, 
  isHazardous: Boolean = false, isActive: Boolean = true)
  extends ModelWithIdParameter[Sku]

object Sku {
  val skuCodeRegex = """([a-zA-Z0-9-_]*)""".r
}

// This table mostly acts a placeholder in our system.  We may or may not import skus from 'origin' into this.
class Skus(tag: Tag) extends GenericTable.TableWithId[Sku](tag, "skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def code = column[String]("code")
  def productId = column[Int]("product_id")
  def attributes = column[Json]("attributes")
  def isHazardous = column[Boolean]("is_hazardous")
  def isActive = column[Boolean]("is_active")

  def * = (id, code, productId, attributes, isHazardous, isActive) <> ((Sku.apply _).tupled, Sku.unapply)

  def product = foreignKey(Products.tableName, productId, Products)(_.id)
}

object Skus extends TableQueryWithId[Sku, Skus](
  idLens = GenLens[Sku](_.id)
  )(new Skus(_))
  with SearchByCode[Sku, Skus] {

  def findOneByCode(code: String): DBIO[Option[Sku]] = filter(_.code === code).one

}
