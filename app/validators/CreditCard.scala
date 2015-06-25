package validators

import com.wix.accord.BaseValidator
import com.wix.accord.ViolationBuilder._
import com.wix.accord.dsl._
import org.joda.time.DateTime

object CreditCard {
  def expirationMonth = new BaseValidator[Int]({ month ⇒
    month >= DateTime.now().getMonthOfYear
  }, _ → s"is in the past")

  def monthOfYear = new BaseValidator[Int]({ month ⇒
    month >= 1 && month <= 12
  }, _ → s"is not a month of the year" )

  def expirationYear = new BaseValidator[Int]({ year ⇒
    year >= DateTime.now().getYear()
  }, _ → s"is in the past")

  def withinTwentyYears = new BaseValidator[Int]({ year ⇒
    val currentYear = DateTime.now().getYear()
    year <= currentYear + 20
  }, _ → s"is too far in the future")
}
