package models.image

import java.time.Instant
import shapeless._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class AlbumImageLink(id: Int = 0,
                          leftId: Int,
                          rightId: Int,
                          createdAt: Instant = Instant.now,
                          updatedAt: Instant = Instant.now)
    extends FoxModel[AlbumImageLink]

class AlbumImageLinks(tag: Tag) extends FoxTable[AlbumImageLink](tag, "album_image_links") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId    = column[Int]("left_id")
  def rightId   = column[Int]("right_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((AlbumImageLink.apply _).tupled, AlbumImageLink.unapply)

  def left  = foreignKey(Albums.tableName, leftId, Albums)(_.id)
  def right = foreignKey(Images.tableName, rightId, Images)(_.id)
}

object AlbumImageLinks
    extends FoxTableQuery[AlbumImageLink, AlbumImageLinks](new AlbumImageLinks(_))
    with ReturningId[AlbumImageLink, AlbumImageLinks] {

  val returningLens: Lens[AlbumImageLink, Int] = lens[AlbumImageLink].id
  def filterLeft(leftId: Int): QuerySeq = filter(_.leftId === leftId)
}
