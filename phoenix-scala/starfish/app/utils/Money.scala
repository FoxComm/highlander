package utils

import cats.implicits._
import org.joda.money.CurrencyUnit
import org.json4s.CustomSerializer
import org.json4s.JsonAST._
import slick.ast
import slick.ast.{BaseTypedType, ScalaType, Type}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, JdbcType}
import slick.util.ConstArray

import scala.language.implicitConversions

// ALWAYS use BigMoney
// Note: type aliases for simple types wont work with json4s https://github.com/json4s/json4s/issues/397 @aafa
object Money {
  type Currency = CurrencyUnit
  type Price    = (Long, Currency) // todo might be a good idea to use it everywhere @aafa

  implicit class RichLong(val value: Long) extends AnyVal {
    def zeroMax: Long                 = Math.max(0L, value)
    def applyTaxes(tax: Double): Long = (value.toDouble * tax).toLong
  }

  type BadCurrency = org.joda.money.IllegalCurrencyException

  object Currency {
    val USD = Currency("USD")
    val RUB = Currency("RUB")

    def apply(s: String): Currency =
      Either.catchOnly[BadCurrency] { CurrencyUnit.of(s.toUpperCase()) }.fold(_ ⇒ USD, identity)

    def unapply(c: Currency): Option[String] = Some(c.getCode)
  }

  val currencyColumnType: JdbcType[Currency] with BaseTypedType[Currency] =
    MappedColumnType.base[Currency, String]({ c ⇒
      c.getCode
    }, { s ⇒
      Currency(s)
    })

  /**
    * Json4s works by matching types against Any at runtime so we need to support these features.
    */
  val jsonFormat = new CustomSerializer[Currency](format ⇒
        ({
      case JString(str) ⇒ Currency(str.toUpperCase)
    }, {
      case c: Currency ⇒ JString(c.getCode)
    }))

}
