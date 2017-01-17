package utils.apis

import failures.CreditCardFailures._
import failures.Failure

private[utils] object StripeMappings {

  val cardExceptionMap: Map[String, Failure] = Map(
    "invalid_number"       → InvalidNumber,
    "invalid_expiry_month" → MonthExpirationInvalid,
    "invalid_expiry_year"  → YearExpirationInvalid,
    "invalid_cvc"          → InvalidCvc,
    "incorrect_number"     → IncorrectNumber,
    "expired_card"         → ExpiredCard,
    "incorrect_cvc"        → IncorrectCvc,
    "incorrect_zip"        → IncorrectZip,
    "card_declined"        → CardDeclined,
    "missing"              → Missing,
    "processing_error"     → ProcessingError
  )

}
