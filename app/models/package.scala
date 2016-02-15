import utils.Money
import utils.time.JavaTimeSlickMapper

package object models {
  implicit val javaTimeSlickMapper      = JavaTimeSlickMapper.instantAndTimestampWithoutZone
  implicit val currencyColumnTypeMapper = Money.currencyColumnType

  object stripe {
    import com.stripe.model._
    type StripeCustomer = Customer
    type StripeCard     = Card
    type StripeCharge   = Charge
  }
}
