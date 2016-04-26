package models.inventory

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class Warehouse(id: Int = 0, name: String) extends FoxModel[Warehouse]

class Warehouses(tag: Tag)
  extends FoxTable[Warehouse](tag, "warehouses")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  def * = (id, name) <> (( Warehouse.apply _).tupled, Warehouse.unapply)
}

object Warehouse {
  def buildDefault(): Warehouse = Warehouse( id = 1, name = "default")

  val HARDCODED_WAREHOUSE_ID = 1
}

object Warehouses extends FoxTableQuery[Warehouse, Warehouses](idLens = lens[Warehouse].id
)(new Warehouses(_)) {

  def findByName(name: String): Query[Warehouses, Warehouse, Seq] =
    filter(_.name === name)
}
