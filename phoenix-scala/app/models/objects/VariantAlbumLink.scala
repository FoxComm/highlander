package models.objects

import java.time.Instant

import models.image.{Album, Albums}
import models.inventory._
import models.objects.ObjectHeadLinks._
import shapeless._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class VariantAlbumLink(id: Int = 0,
                            leftId: Int,
                            rightId: Int,
                            position: Int = 0,
                            createdAt: Instant = Instant.now,
                            updatedAt: Instant = Instant.now)
    extends FoxModel[VariantAlbumLink]
    with OrderedObjectHeadLink[VariantAlbumLink] {
  override def withPosition(newPosition: Id): VariantAlbumLink = copy(position = newPosition)
}

class VariantAlbumLinks(tag: Tag)
    extends OrderedObjectHeadLinks[VariantAlbumLink](tag, "variant_album_links") {

  def * =
    (id, leftId, rightId, position, createdAt, updatedAt) <> ((VariantAlbumLink.apply _).tupled, VariantAlbumLink.unapply)

  def left  = foreignKey(ProductVariants.tableName, leftId, ProductVariants)(_.id)
  def right = foreignKey(Albums.tableName, rightId, Albums)(_.id)
}

object VariantAlbumLinks
    extends OrderedObjectHeadLinkQueries[VariantAlbumLink,
                                         VariantAlbumLinks,
                                         ProductVariant,
                                         Album](new VariantAlbumLinks(_), ProductVariants, Albums)
    with ReturningId[VariantAlbumLink, VariantAlbumLinks] {

  val returningLens: Lens[VariantAlbumLink, Int] = lens[VariantAlbumLink].id

  def buildOrdered(left: ProductVariant, right: Album, position: Int) =
    VariantAlbumLink(leftId = left.id, rightId = right.id, position = position)
}
