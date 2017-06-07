package phoenix.models.objects

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import objectframework.models._
import phoenix.models.image.{Album, Albums}
import phoenix.models.inventory._
import shapeless._

case class SkuAlbumLink(id: Int = 0,
                        leftId: Int,
                        rightId: Int,
                        position: Int = 0,
                        createdAt: Instant = Instant.now,
                        updatedAt: Instant = Instant.now,
                        archivedAt: Option[Instant] = None)
    extends FoxModel[SkuAlbumLink]
    with OrderedObjectHeadLink[SkuAlbumLink] {
  override def withPosition(newPosition: Id): SkuAlbumLink = copy(position = newPosition)
}

class SkuAlbumLinks(tag: Tag) extends OrderedObjectHeadLinks[SkuAlbumLink](tag, "sku_album_links") {

  def * =
    (id, leftId, rightId, position, createdAt, updatedAt, archivedAt) <> ((SkuAlbumLink.apply _).tupled, SkuAlbumLink.unapply)

  def left  = foreignKey(Skus.tableName, leftId, Skus)(_.id)
  def right = foreignKey(Albums.tableName, rightId, Albums)(_.id)
}

object SkuAlbumLinks
    extends OrderedObjectHeadLinkQueries[SkuAlbumLink, SkuAlbumLinks, Sku, Album](new SkuAlbumLinks(_),
                                                                                  Skus,
                                                                                  Albums)
    with ReturningId[SkuAlbumLink, SkuAlbumLinks] {

  val returningLens: Lens[SkuAlbumLink, Int] = lens[SkuAlbumLink].id

  def buildOrdered(left: Sku, right: Album, position: Int) =
    SkuAlbumLink(leftId = left.id, rightId = right.id, position = position)
}
