package phoenix.services.activity

import phoenix.responses.users.UserResponse
import phoenix.responses.{AddressResponse, CreditCardNoAddressResponse}

object UserTailored {
  case class UserCreated(creator: UserResponse, user: UserResponse) extends ActivityBase[UserCreated]

  case class UserRegistered(user: UserResponse) extends ActivityBase[UserRegistered]

  case class UserActivated(admin: UserResponse, user: UserResponse) extends ActivityBase[UserActivated]

  case class UserBlacklisted(admin: UserResponse, user: UserResponse) extends ActivityBase[UserBlacklisted]

  case class UserRemovedFromBlacklist(admin: UserResponse, user: UserResponse)
      extends ActivityBase[UserRemovedFromBlacklist]

  case class UserEnabled(admin: UserResponse, user: UserResponse) extends ActivityBase[UserEnabled]

  case class UserDisabled(admin: UserResponse, user: UserResponse) extends ActivityBase[UserDisabled]

  case class UserRemindPassword(user: UserResponse, code: String, isAdmin: Boolean)
      extends ActivityBase[UserRemindPassword]

  case class UserPasswordReset(user: UserResponse) extends ActivityBase[UserPasswordReset]

  case class UserPasswordChanged(user: UserResponse) extends ActivityBase[UserPasswordChanged]

  case class UserUpdated(oldInfo: UserResponse, newInfo: UserResponse, admin: Option[UserResponse] = None)
      extends ActivityBase[UserUpdated]

  /* User Addresses */
  case class UserAddressCreated(user: UserResponse, address: AddressResponse, admin: Option[UserResponse])
      extends ActivityBase[UserAddressCreated]

  case class UserAddressUpdated(user: UserResponse,
                                oldInfo: AddressResponse,
                                newInfo: AddressResponse,
                                admin: Option[UserResponse])
      extends ActivityBase[UserAddressUpdated]

  case class UserAddressDeleted(user: UserResponse, address: AddressResponse, admin: Option[UserResponse])
      extends ActivityBase[UserAddressDeleted]

  /* User Credit Cards */
  case class CreditCardAdded(user: UserResponse,
                             creditCard: CreditCardNoAddressResponse,
                             admin: Option[UserResponse])
      extends ActivityBase[CreditCardAdded]

  case class CreditCardUpdated(user: UserResponse,
                               oldInfo: CreditCardNoAddressResponse,
                               newInfo: CreditCardNoAddressResponse,
                               admin: Option[UserResponse])
      extends ActivityBase[CreditCardUpdated]

  case class CreditCardRemoved(user: UserResponse,
                               creditCard: CreditCardNoAddressResponse,
                               admin: Option[UserResponse])
      extends ActivityBase[CreditCardRemoved]
}
