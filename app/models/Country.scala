package models

import java.io.{FilenameFilter, File}
import java.nio.file.{Paths, SimpleFileVisitor, Files, Path}

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.Money._
import utils.{ModelWithIdParameter, RichTable, TableQueryWithId}
import Country._

final case class Country(id: Int = 0, name: String, alpha2: Alpha2, alpha3: Alpha3, code: Option[Code],
  continent: String, currency: Currency, languages: Language, postalCode: Boolean)
  extends ModelWithIdParameter {
}

object Country {
  type Code = String
  type Alpha2 = String
  type Alpha3 = String
  type Language = String
}

class Countries(tag: Tag) extends TableWithId[Country](tag, "countries") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def alpha2 = column[Alpha2]("alpha2")
  def alpha3 = column[Alpha3]("alpha3")
  def code = column[Option[Code]]("code")
  def continent = column[String]("continent")
  def currency = column[Currency]("currency")
  def languages = column[String]("languages")
  def postalCode = column[Boolean]("postal_code")

  def * = (id, name, alpha2, alpha3, code, continent, currency,
    languages, postalCode) <> ((Country.apply _).tupled, Country.unapply)
}

object Countries extends TableQueryWithId[Country, Countries](
  idLens = GenLens[Country](_.id)
)(new Countries(_)) {
}
