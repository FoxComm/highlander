package phoenix.responses

import java.time.Instant

import phoenix.models.account._

object UserResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  createdAt: Instant,
                  disabled: Boolean,
                  isBlacklisted: Boolean)
      extends ResponseItem

  def build(user: User): Root = {
    Root(id = user.accountId,
         email = user.email,
         name = user.name,
         phoneNumber = user.phoneNumber,
         createdAt = user.createdAt,
         disabled = user.isDisabled,
         isBlacklisted = user.isBlacklisted)
  }

  case class ResetPasswordSendAnswer(status: String)

  case class ResetPasswordDoneAnswer(email: String, org: String)

}
