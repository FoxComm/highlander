package models.location

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class Region(id: Int = 0, countryId: Int, name: String, abbreviation: Option[String] = None)
    extends FoxModel[Region] {
  val abbrev = abbreviation
}

object Region {
  val usRegions        = 4121 to 4180
  val armedRegions     = Seq(4121, 4122, 4125)
  val regularUsRegions = usRegions.toSeq.diff(armedRegions)
}

class Regions(tag: Tag) extends FoxTable[Region](tag, "regions") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def countryId    = column[Int]("country_id")
  def name         = column[String]("name")
  def abbreviation = column[Option[String]]("abbreviation")

  def * = (id, countryId, name, abbreviation) <> ((Region.apply _).tupled, Region.unapply)

  def country = foreignKey(Countries.tableName, countryId, Countries)(_.id)
}

object Regions
    extends FoxTableQuery[Region, Regions](new Regions(_))
    with ReturningId[Region, Regions] {
  val returningLens: Lens[Region, Int] = lens[Region].id
}
