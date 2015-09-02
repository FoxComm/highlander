package models

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.GenericTable.TableWithId
import utils.Money._
import utils.{ModelWithIdParameter, TableQueryWithId}

final case class Country(id: Int = 0, name: String, alpha2: String, alpha3: String, code: Option[String],
  continent: String, currency: Currency, languages: List[String],
  usesPostalCode: Boolean = false, isShippable: Boolean = false, isBillable: Boolean = false)
  extends ModelWithIdParameter {
}

object Country {
  val unitedStatesId: Int = 234
  val usRegions: Range    = (4121 to 4180).toSeq
}

class Countries(tag: Tag) extends TableWithId[Country](tag, "countries")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def alpha2 = column[String]("alpha2")
  def alpha3 = column[String]("alpha3")
  def code = column[Option[String]]("code")
  def continent = column[String]("continent")
  def currency = column[Currency]("currency")
  def languages = column[List[String]]("languages")
  def usesPostalCode = column[Boolean]("uses_postal_code")
  def isShippable = column[Boolean]("is_shippable")
  def isBillable = column[Boolean]("is_billable")

  def * = (id, name, alpha2, alpha3, code, continent, currency,
    languages, usesPostalCode, isShippable, isBillable) <> ((Country.apply _).tupled, Country.unapply)
}

object Countries extends TableQueryWithId[Country, Countries](
  idLens = GenLens[Country](_.id)
)(new Countries(_)) {
}
