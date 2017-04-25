package utils

import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.joda.money.CurrencyUnit
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType

// ALWAYS use BigMoney
object Money {
  type Currency = CurrencyUnit

  type BadCurrency = org.joda.money.IllegalCurrencyException

  object Currency {
    implicit val decodeCurrency: Decoder[Currency] =
      Decoder.decodeString.map(s ⇒ Currency(s.toUpperCase()))
    implicit val encodeCurrency: Encoder[Currency] = Encoder.encodeString.contramap(_.getCode)

    val USD = Currency("USD")
    val RUB = Currency("RUB")

    // TODO: do we really want to default to any particular currency on (deserialization) error ?
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
}
