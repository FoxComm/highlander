package models

import monocle.macros.GenLens
import utils.GenericTable.TableWithId
import utils.{TableQueryWithId, ModelWithIdParameter, RichTable, RunOnDbIO, Model}

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class Region(id: Int = 0, countryId: Int, name: String, abbreviation: String)
  extends ModelWithIdParameter {
  val abbrev = abbreviation
}

class Regions(tag: Tag) extends TableWithId[Region](tag, "regions") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def countryId = column[Int]("country_id")
  def name = column[String]("name")
  def abbreviation = column[String]("abbreviation")

  def * = (id, countryId, name, abbreviation) <> ((Region.apply _).tupled, Region.unapply)
}

object Regions extends TableQueryWithId[Region, Regions](
  idLens = GenLens[Region](_.id)
)(new Regions(_)) {
}
