package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import utils.Validation
import utils.Validation._

object UserPayloads {

  case class ToggleUserDisabled(disabled: Boolean)

  // TODO: add blacklistedReason later
  case class ToggleUserBlacklisted(blacklisted: Boolean)

  // Reset password payloads
  case class ResetPasswordSend(email: String) extends Validation[ResetPasswordSend] {
    def validate: ValidatedNel[Failure, ResetPasswordSend] = {
      notEmpty(email, "email").map { _ ⇒
        this
      }
    }
  }

  case class ResetPassword(code: String, newPassword: String) extends Validation[ResetPassword] {
    def validate: ValidatedNel[Failure, ResetPassword] =
      (notEmpty(code, "code") |@| notEmpty(newPassword, "password")).map { case _ ⇒ this }
  }
}