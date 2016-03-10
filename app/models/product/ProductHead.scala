package models.product

import java.time.Instant

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A ProductHead is a pointer to a commit of a product. A ProductContext is a
 * collection of ProductHeads.
 */
final case class ProductHead(id: Int = 0, contextId: Int, shadowId: Int, productId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ProductHead]
  with Validation[ProductHead]

class ProductHeads(tag: Tag) extends GenericTable.TableWithId[ProductHead](tag, "product_heads")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def contextId = column[Int]("context_id")
  def shadowId = column[Int]("shadow_id")
  def productId = column[Int]("product_id")
  def commitId = column[Int]("commit_id")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def * = (id, contextId, shadowId, productId, commitId, updatedAt, createdAt) <> ((ProductHead.apply _).tupled, ProductHead.unapply)

  def context = foreignKey(ProductContexts.tableName, contextId, ProductContexts)(_.id)
  def shadow = foreignKey(ProductShadows.tableName, shadowId, ProductShadows)(_.id)
  def product = foreignKey(Products.tableName, productId, Products)(_.id)
  def commit = foreignKey(ProductCommits.tableName, commitId, ProductCommits)(_.id)

}

object ProductHeads extends TableQueryWithId[ProductHead, ProductHeads](
  idLens = GenLens[ProductHead](_.id))(new ProductHeads(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)
}
