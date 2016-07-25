package models.objects

import java.time.Instant

import models.image.{Album, Albums}
import models.inventory._
import models.objects.ObjectHeadLinks._
import shapeless._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class SkuAlbumLink(id: Int = 0,
                        leftId: Int,
                        rightId: Int,
                        createdAt: Instant = Instant.now,
                        updatedAt: Instant = Instant.now)
    extends FoxModel[SkuAlbumLink]
    with ObjectHeadLink[SkuAlbumLink]

class SkuAlbumLinks(tag: Tag) extends ObjectHeadLinks[SkuAlbumLink](tag, "sku_album_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((SkuAlbumLink.apply _).tupled, SkuAlbumLink.unapply)

  def left  = foreignKey(Skus.tableName, leftId, Skus)(_.id)
  def right = foreignKey(Albums.tableName, rightId, Albums)(_.id)
}

object SkuAlbumLinks
    extends ObjectHeadLinkQueries[SkuAlbumLink, SkuAlbumLinks, Sku, Album](new SkuAlbumLinks(_),
                                                                           Skus,
                                                                           Albums)
    with ReturningId[SkuAlbumLink, SkuAlbumLinks] {

  val returningLens: Lens[SkuAlbumLink, Int] = lens[SkuAlbumLink].id
}
