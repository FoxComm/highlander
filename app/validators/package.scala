import com.wix.accord.BaseValidator

import com.wix.accord.ViolationBuilder._
import scala.language.reflectiveCalls

package object validators {
  import Validators._

  def notExpired[T](year: Int, month: Int) = CreditCard.notExpired[T](year, month)

  def withinTwentyYears[T](year: Int, month: Int) = CreditCard.withinTwentyYears[T](year, month)

  def nonEmptyIf[T](bool: Boolean, option: Option[_]) = new NonEmptyIf[T](bool, option)
}

object Validators {
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any"))
  class NonEmptyIf[T](bool: Boolean, option: Option[_])
    extends BaseValidator[T]({ _ ⇒
      (bool, option) match {
        case (true, Some(_)) ⇒ true
        case (true, None) ⇒ false
        case _ ⇒ true
      }
    }, _ -> s"must not be empty")
}
