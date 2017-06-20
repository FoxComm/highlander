package phoenix.models.location

import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Money._
import shapeless._

case class Country(id: Int = 0,
                   name: String,
                   alpha2: String,
                   alpha3: String,
                   code: Option[String],
                   continent: String,
                   currency: Currency,
                   languages: List[String],
                   usesPostalCode: Boolean = false,
                   isShippable: Boolean = false,
                   isBillable: Boolean = false)
    extends FoxModel[Country] {}

object Country {
  val unitedStatesId: Int = 234
  val countryCodeRegex    = """([a-zA-Z]{2,3})""".r
}

class Countries(tag: Tag) extends FoxTable[Country](tag, "countries") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name           = column[String]("name")
  def alpha2         = column[String]("alpha2")
  def alpha3         = column[String]("alpha3")
  def code           = column[Option[String]]("code")
  def continent      = column[String]("continent")
  def currency       = column[Currency]("currency")
  def languages      = column[List[String]]("languages")
  def usesPostalCode = column[Boolean]("uses_postal_code")
  def isShippable    = column[Boolean]("is_shippable")
  def isBillable     = column[Boolean]("is_billable")

  def * =
    (id, name, alpha2, alpha3, code, continent, currency, languages, usesPostalCode, isShippable, isBillable) <> ((Country.apply _).tupled, Country.unapply)
}

object Countries
    extends FoxTableQuery[Country, Countries](new Countries(_))
    with ReturningId[Country, Countries] {
  val returningLens: Lens[Country, Int] = lens[Country].id

  // Query for both 2- and 3-lettered code for convenience @aafa
  def findByCode(code: String): QuerySeq =
    filter(c ⇒ c.alpha2.toUpperCase === code.toUpperCase || c.alpha3.toUpperCase === code.toUpperCase)
}
