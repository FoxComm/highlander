package utils

import org.joda.money.CurrencyUnit
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString
import slick.driver.PostgresDriver.api._

// ALWAYS use BigMoney
object Money {
  type Currency = CurrencyUnit

  object Currency {
    val USD = Currency("USD")

    def apply(s: String): Currency = CurrencyUnit.of(s)
    def unapply(c: Currency): Option[String] = Some(c.getCode)
  }

  implicit val currencyColumnType = MappedColumnType.base[Currency, String](
    { c => c.getCode },
    { s => Currency(s) })

  val jsonFormat = new CustomSerializer[Currency](format => ({
    case JString(str) ⇒ Currency(str.toUpperCase)
  }, {
    case c: Currency ⇒ JString(c.getCode)
  }))
}
