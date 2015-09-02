package validators

import com.wix.accord.BaseValidator
import com.wix.accord.ViolationBuilder._
import org.joda.time.DateTime

object CreditCard {
  final case class Expiraton(year: Int, month: Int)

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any"))
  class ExpiredCard[T](exp: Expiraton)
    extends BaseValidator[T]({ _ ⇒
      val today = DateTime.now()
      try {
        val expDate = new DateTime(exp.year, exp.month, 1, 0, 0).plusMonths(1).minusSeconds(1)
        expDate.isEqual(today) || expDate.isAfter(today)
      } catch {
        case _: IllegalArgumentException ⇒ false
      }
    }, _ -> s"is expired")

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any"))
  class WithinTwentyYears[T](exp: Expiraton)
    extends BaseValidator[T]({ _ ⇒
      val today = DateTime.now()
      try {
        // At the end of the month
        val expDate = new DateTime(exp.year, exp.month, 1, 0, 0).plusMonths(1).minusSeconds(1)
        expDate.isBefore(today.plusYears(20))
      } catch {
        case _: IllegalArgumentException ⇒ false
      }
  }, _ -> s"expiration is too far in the future" )

  def notExpired[T](year: Int, month: Int) = new ExpiredCard[T](Expiraton(year = year, month = month))

  def withinTwentyYears[T](year: Int, month: Int) = new WithinTwentyYears[T](Expiraton(year = year, month = month))
}

