package models.objects

import java.time.Instant

import com.pellucid.sealerate
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
 * A ObjectLink connects a SKU to a product. A SKU may be part of more
 * than one product. For example, a SKU can be part of a bundle but also
 * sold separately. Or a SKU might not be part of any product yet if it was
 * imported from a 3rd party system.
 *
 */
case class ObjectLink(id: Int = 0, leftId: Int, rightId: Int, linkType: ObjectLink.LinkType,
  createdAt: Instant = Instant.now, updatedAt: Instant = Instant.now)
  extends FoxModel[ObjectLink]

object ObjectLink {
  sealed trait LinkType

  case object ProductAlbum extends LinkType
  case object ProductSku extends LinkType
  case object PromotionDiscount extends LinkType
  case object SkuAlbum extends LinkType

  object LinkType extends ADT[LinkType] {
    def types = sealerate.values[LinkType]
  }

  implicit val linkTypeColumnType: JdbcType[LinkType] with BaseTypedType[LinkType] =
    LinkType.slickColumn
}

class ObjectLinks(tag: Tag) extends FoxTable[ObjectLink](tag, "object_links")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId = column[Int]("left_id")
  def rightId = column[Int]("right_id")
  def linkType = column[ObjectLink.LinkType]("link_type")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * = (id, leftId, rightId, linkType, createdAt, updatedAt) <> (
    (ObjectLink.apply _).tupled, ObjectLink.unapply)

  def left = foreignKey(ObjectShadows.tableName, leftId, ObjectShadows)(_.id)
  def right = foreignKey(ObjectShadows.tableName, rightId, ObjectShadows)(_.id)
}

object ObjectLinks extends FoxTableQuery[ObjectLink, ObjectLinks](new ObjectLinks(_))
  with ReturningId[ObjectLink, ObjectLinks] {

  val returningLens: Lens[ObjectLink, Int] = lens[ObjectLink].id

  def findByLeftRight(leftId: Int, rightId: Int): QuerySeq =
    filter(_.leftId === leftId).filter(_.rightId === rightId)
  def findByLeft(leftId: Int): QuerySeq =
    filter(_.leftId === leftId)
  def findByRight(rightId: Int): QuerySeq =
    filter(_.rightId === rightId)
  def findByLeftAndType(leftId: Int, linkType: ObjectLink.LinkType): QuerySeq =
    filter(_.leftId === leftId).filter(_.linkType === linkType)

}
