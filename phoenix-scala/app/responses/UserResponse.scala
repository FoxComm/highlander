package responses

import java.time.Instant

import models.account._
import models.location.Region

object UserResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  createdAt: Instant,
                  disabled: Boolean,
                  blacklisted: Boolean)
      extends ResponseItem

  def build(user: User): Root =
    Root(id = user.id,
         email = user.email,
         name = user.name,
         phoneNumber = user.phoneNumber,
         createdAt = user.createdAt,
         disabled = user.isDisabled,
         blacklisted = user.isBlacklisted)

  case class ResetPasswordSendAnswer(status: String)

  case class ResetPasswordDoneAnswer(status: String)

}
