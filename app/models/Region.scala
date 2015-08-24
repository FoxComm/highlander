package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, RichTable, TableQueryWithId}

final case class Region(id: Int = 0, countryId: Int, name: String, abbreviation: Option[String] = None)
  extends ModelWithIdParameter {
  val abbrev = abbreviation
}

class Regions(tag: Tag) extends TableWithId[Region](tag, "regions") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def countryId = column[Int]("country_id")
  def name = column[String]("name")
  def abbreviation = column[Option[String]]("abbreviation")

  def * = (id, countryId, name, abbreviation) <> ((Region.apply _).tupled, Region.unapply)

  def country = foreignKey(Countries.tableName, countryId, Countries)(_.id)
}

object Regions extends TableQueryWithId[Region, Regions](
  idLens = GenLens[Region](_.id)
)(new Regions(_)) {
}
