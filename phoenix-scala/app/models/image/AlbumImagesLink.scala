package models.image

import java.time.Instant
import models.objects.ObjectHeadLinks._
import models.objects.{OrderedObjectHeadLinkQueries, OrderedObjectHeadLinks, OrderedObjectHeadLink}
import shapeless._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class AlbumImageLink(id: Int = 0,
                          leftId: Int,
                          rightId: Int,
                          position: Int = 0,
                          createdAt: Instant = Instant.now,
                          updatedAt: Instant = Instant.now)
    extends FoxModel[AlbumImageLink]
    with OrderedObjectHeadLink[AlbumImageLink] {
  override def withPosition(newPosition: Id): AlbumImageLink = copy(position = newPosition)
}

class AlbumImageLinks(tag: Tag)
    extends OrderedObjectHeadLinks[AlbumImageLink](tag, "album_image_links") {

  def * =
    (id, leftId, rightId, position, createdAt, updatedAt) <> ((AlbumImageLink.apply _).tupled, AlbumImageLink.unapply)

  def left  = foreignKey(Albums.tableName, leftId, Albums)(_.id)
  def right = foreignKey(Images.tableName, rightId, Images)(_.id)
}

object AlbumImageLinks
    extends OrderedObjectHeadLinkQueries[AlbumImageLink, AlbumImageLinks, Album, Image](
      new AlbumImageLinks(_),
      Albums,
      Images)
    with ReturningId[AlbumImageLink, AlbumImageLinks] {

  val returningLens: Lens[AlbumImageLink, Int] = lens[AlbumImageLink].id

  def filterLeftAndRight(leftId: Int, rightId: Int): QuerySeq =
    filter(link ⇒ link.leftId === leftId && link.rightId === rightId)

  def buildOrdered(left: Album, right: Image, position: Int) =
    AlbumImageLink(leftId = left.id, rightId = right.id, position = position)
}
