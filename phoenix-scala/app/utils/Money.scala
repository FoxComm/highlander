package utils

import cats.implicits._
import org.joda.money.CurrencyUnit
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType

// ALWAYS use BigMoney
object Money {
  type Currency = CurrencyUnit

  type BadCurrency = org.joda.money.IllegalCurrencyException

  object Currency {
    val USD = Currency("USD")
    val RUB = Currency("RUB")

    // TODO: do we really want to default to any particular currency on (deserialization) error ?
    def apply(s: String): Currency =
      Either.catchOnly[BadCurrency] { CurrencyUnit.of(s.toUpperCase()) }.fold(_ ⇒ USD, identity)

    def unapply(c: Currency): Option[String] = Some(c.getCode)
  }

  val currencyColumnType: JdbcType[Currency] with BaseTypedType[Currency] =
    MappedColumnType.base[Currency, String](_.getCode, Currency(_))
}
