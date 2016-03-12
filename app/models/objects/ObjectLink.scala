package models.objects

import models.Aliases.Json
import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.Slick.implicits._
import utils.table.SearchByCode
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.time.JavaTimeSlickMapper._
import java.time.Instant

/**
 * A ObjectLink connects a SKU to a product. A SKU may be part of more 
 * than one product. For example, a SKU can be part of a bundle but also
 * sold separately. Or a SKU might not be part of any product yet if it was
 * imported from a 3rd party system.
 *
 */
final case class ObjectLink(id: Int = 0, leftId: Int, rightId: Int)
  extends ModelWithIdParameter[ObjectLink]

class ObjectLinks(tag: Tag) extends GenericTable.TableWithId[ObjectLink](tag, "object_links")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId = column[Int]("left_id")
  def rightId = column[Int]("right_id")

  def * = (id, leftId, rightId) <> ((ObjectLink.apply _).tupled, ObjectLink.unapply)

  def left = foreignKey(ObjectShadows.tableName, leftId, ObjectShadows)(_.id)
  def right = foreignKey(ObjectShadows.tableName, rightId, ObjectShadows)(_.id)
}

object ObjectLinks extends TableQueryWithId[ObjectLink, ObjectLinks](
  idLens = GenLens[ObjectLink](_.id)
  )(new ObjectLinks(_)) {

  def findByLeft(leftId: Int): QuerySeq = 
    filter(_.leftId === leftId)
  def findByRight(rightId: Int): QuerySeq = 
    filter(_.rightId === rightId)

}
