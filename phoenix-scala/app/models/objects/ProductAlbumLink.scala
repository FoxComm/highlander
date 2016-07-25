package models.objects

import java.time.Instant

import models.image.{Album, Albums}
import models.objects.ObjectHeadLinks._
import shapeless._
import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class ProductAlbumLink(id: Int = 0,
                            leftId: Int,
                            rightId: Int,
                            createdAt: Instant = Instant.now,
                            updatedAt: Instant = Instant.now)
    extends FoxModel[ProductAlbumLink]
    with ObjectHeadLink[ProductAlbumLink]

class ProductAlbumLinks(tag: Tag)
    extends ObjectHeadLinks[ProductAlbumLink](tag, "product_album_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductAlbumLink.apply _).tupled, ProductAlbumLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Albums.tableName, rightId, Albums)(_.id)
}

object ProductAlbumLinks
    extends ObjectHeadLinkQueries[ProductAlbumLink, ProductAlbumLinks, Product, Album](
        new ProductAlbumLinks(_),
        Products,
        Albums)
    with ReturningId[ProductAlbumLink, ProductAlbumLinks] {

  val returningLens: Lens[ProductAlbumLink, Int] = lens[ProductAlbumLink].id
}
