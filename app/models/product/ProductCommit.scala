package models.product

import java.time.Instant

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

/**
 * A ProductCommit is a tree of commits, each pointing to a product shadow.
 */
final case class ProductCommit(id: Int = 0, shadowId: Int, productId: Int, 
  reasonId: Option[Int] = None, previousId: Option[Int] = None, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[ProductCommit]
  with Validation[ProductCommit]

class ProductCommits(tag: Tag) extends GenericTable.TableWithId[ProductCommit](tag, "product_commits")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def shadowId = column[Int]("shadow_id")
  def productId = column[Int]("product_id")
  def reasonId = column[Option[Int]]("reason_id")
  def previousId = column[Option[Int]]("previous_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, shadowId, productId, reasonId, previousId, createdAt) <> ((ProductCommit.apply _).tupled, ProductCommit.unapply)

  def shadow = foreignKey(ProductShadows.tableName, shadowId, ProductShadows)(_.id)
  def product = foreignKey(Products.tableName, productId, Products)(_.id)
  def previousCommit = foreignKey(ProductCommits.tableName, previousId, ProductCommits)(_.id)

}

object ProductCommits extends TableQueryWithId[ProductCommit, ProductCommits](
  idLens = GenLens[ProductCommit](_.id))(new ProductCommits(_)) {

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByProduct(productId: Int): QuerySeq = 
    filter(_.productId === productId)
}
