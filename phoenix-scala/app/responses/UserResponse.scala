package responses

import io.circe.syntax._
import java.time.Instant
import models.account._
import utils.aliases._
import utils.json.codecs._

object UserResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  createdAt: Instant,
                  disabled: Boolean,
                  isBlacklisted: Boolean)
      extends ResponseItem {
    def json: Json = this.asJson
  }

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

  case class ResetPasswordDoneAnswer(status: String)

}
