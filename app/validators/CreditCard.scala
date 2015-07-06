package validators

import com.wix.accord.{Validator, BaseValidator}
import com.wix.accord.ViolationBuilder._
import org.joda.time.DateTime

object CreditCard {
  final case class Expiraton(year: Int, month: Int)

  class ExpiredCard[T](exp: Expiraton)
    extends BaseValidator[T]({ _ ⇒
      val today = DateTime.now()
      try {
        val expDate = today.withDate(exp.year, exp.month, today.getDayOfMonth)
        expDate.isEqual(today) || expDate.isAfter(today)
      } catch {
        case _: IllegalArgumentException ⇒ false
      }
    }, _ -> s"is expired" )

  class WithinTwentyYears[T](exp: Expiraton)
    extends BaseValidator[T]({ _ ⇒
      val today = DateTime.now()
      try {
        val expDate = new DateTime(exp.year, exp.month, today.getDayOfMonth, today.getHourOfDay, today.getMinuteOfHour)
        expDate.isBefore(today.plusYears(20))
      } catch {
        case _: IllegalArgumentException ⇒ false
      }
  }, _ -> s"expiration is too far in the future" )

  def notExpired[T](year: Int, month: Int) = new ExpiredCard[T](Expiraton(year = year, month = month))

  def withinTwentyYears[T](year: Int, month: Int) = new WithinTwentyYears[T](Expiraton(year = year, month = month))
}
