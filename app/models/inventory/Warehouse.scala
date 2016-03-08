package models.inventory

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class Warehouse(id: Int = 0, name: String) extends ModelWithIdParameter[Warehouse]

class Warehouses(tag: Tag)
  extends GenericTable.TableWithId[Warehouse](tag, "warehouses")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  def * = (id, name) <> (( Warehouse.apply _).tupled, Warehouse.unapply)
}

object Warehouse {
  def buildDefault(): Warehouse = Warehouse( id = 1, name = "default")

  val HARDCODED_WAREHOUSE_ID = 1
}

object Warehouses extends TableQueryWithId[Warehouse, Warehouses](idLens = GenLens[Warehouse](_.id)
)(new Warehouses(_)) {

  def findByName(name: String): Query[Warehouses, Warehouse, Seq] =
    filter(_.name === name)
}
