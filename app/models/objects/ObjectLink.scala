package models.objects

import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
 * A ObjectLink connects a SKU to a product. A SKU may be part of more 
 * than one product. For example, a SKU can be part of a bundle but also
 * sold separately. Or a SKU might not be part of any product yet if it was
 * imported from a 3rd party system.
 *
 */
case class ObjectLink(id: Int = 0, leftId: Int, rightId: Int)
  extends FoxModel[ObjectLink]

class ObjectLinks(tag: Tag) extends FoxTable[ObjectLink](tag, "object_links")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId = column[Int]("left_id")
  def rightId = column[Int]("right_id")

  def * = (id, leftId, rightId) <> ((ObjectLink.apply _).tupled, ObjectLink.unapply)

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

}
