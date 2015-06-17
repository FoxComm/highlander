package models


import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }

import scala.concurrent.{ExecutionContext, Future}


case class Sku(id: Int = 0, name: String) {

}

// This table mostly acts a placeholder in our system.  We may or may not import skus from 'origin' into this.
class Skus(tag: Tag) extends GenericTable.TableWithId[Sku](tag, "skus") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("string")

  def * = (id, name) <> ((Sku.apply _).tupled, Sku.unapply)


}