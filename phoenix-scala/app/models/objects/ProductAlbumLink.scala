package models.objects

import java.time.Instant

import models.image.{Album, Albums}
import models.objects.ObjectHeadLinks._
import models.product._
import shapeless._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ProductAlbumLink(id: Int = 0,
                            leftId: Int,
                            rightId: Int,
                            position: Int = 0,
                            createdAt: Instant = Instant.now,
                            updatedAt: Instant = Instant.now)
    extends FoxModel[ProductAlbumLink]
    with OrderedObjectHeadLink[ProductAlbumLink] {
  override def withPosition(newPosition: Id): ProductAlbumLink = copy(position = newPosition)
}

class ProductAlbumLinks(tag: Tag)
    extends OrderedObjectHeadLinks[ProductAlbumLink](tag, "product_album_links") {

  def * =
    (id, leftId, rightId, position, createdAt, updatedAt) <> ((ProductAlbumLink.apply _).tupled, ProductAlbumLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Albums.tableName, rightId, Albums)(_.id)
}

object ProductAlbumLinks
    extends OrderedObjectHeadLinkQueries[ProductAlbumLink, ProductAlbumLinks, Product, Album](
      new ProductAlbumLinks(_),
      Products,
      Albums)
    with ReturningId[ProductAlbumLink, ProductAlbumLinks] {

  val returningLens: Lens[ProductAlbumLink, Int] = lens[ProductAlbumLink].id

  def buildOrdered(left: Product, right: Album, position: Int) =
    ProductAlbumLink(leftId = left.id, rightId = right.id, position = position)
}
