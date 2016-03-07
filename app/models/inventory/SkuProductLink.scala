package models.inventory

import models.Aliases.Json
import models.product.Products
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.Slick.implicits._
import utils.table.SearchByCode
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.time.JavaTimeSlickMapper._
import java.time.Instant

/**
 * A SkuProductLink connects a SKU to a product. A SKU may be part of more 
 * than one product. For example, a SKU can be part of a bundle but also
 * sold separately. Or a SKU might not be part of any product yet if it was
 * imported from a 3rd party system.
 *
 */
final case class SkuProductLink(id: Int = 0, skuId: Int, productId: Int)
  extends ModelWithIdParameter[SkuProductLink]

class SkuProductLinks(tag: Tag) extends GenericTable.TableWithId[SkuProductLink](tag, "sku_product_links")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def productId = column[Int]("product_id")

  def * = (id, skuId, productId) <> ((SkuProductLink.apply _).tupled, SkuProductLink.unapply)

  def product = foreignKey(Products.tableName, productId, Products)(_.id)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object SkuProductLinks extends TableQueryWithId[SkuProductLink, SkuProductLinks](
  idLens = GenLens[SkuProductLink](_.id)
  )(new SkuProductLinks(_)) {

  def findBySku(skuId: Int): QuerySeq = 
    filter(_.skuId === skuId)
  def findByProduct(productId: Int): QuerySeq = 
    filter(_.productId === productId)

}
