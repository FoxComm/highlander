package utils

import cats.data.Xor
import org.joda.money.CurrencyUnit
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType

// ALWAYS use BigMoney
object Money {
  type Currency = CurrencyUnit

  type BadCurrency = org.joda.money.IllegalCurrencyException

  object Currency {
    val USD = Currency("USD")

    def apply(s: String): Currency = Xor.fromTryCatch[BadCurrency] { CurrencyUnit.of(s) }.fold(_ ⇒ USD, identity)
    def unapply(c: Currency): Option[String] = Some(c.getCode)
  }

  implicit val currencyColumnType: JdbcType[Currency] with BaseTypedType[Currency] = MappedColumnType.base[Currency, String](
    { c => c.getCode },
    { s => Currency(s) })

  /**
   * Json4s works by matching types against Any at runtime so we need to support these features.
   */
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any", "org.brianmckenna.wartremover.warts.IsInstanceOf"))
  val jsonFormat = new CustomSerializer[Currency](format => ({
    case JString(str) ⇒ Currency(str.toUpperCase)
  }, {
    case c: Currency ⇒ JString(c.getCode)
  }))
}
