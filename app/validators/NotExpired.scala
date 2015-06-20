package validators

import com.wix.accord.BaseValidator
import com.wix.accord.ViolationBuilder._
import org.joda.time.DateTime

class NotExpired extends BaseValidator[Int]({ in ⇒
    /** Year is already restricted */
    val currentMonth = DateTime.now().monthOfYear().get()
    in >= currentMonth
  }, _ → s"card's expiration in the past")

object NotExpired {
  def notExpired = new NotExpired
}
