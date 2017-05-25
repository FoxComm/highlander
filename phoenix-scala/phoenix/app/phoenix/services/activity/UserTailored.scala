package phoenix.services.activity

import phoenix.responses.{AddressResponse, CreditCardsResponse, UserResponse}

object UserTailored {
  case class UserCreated(creator: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserCreated]

  case class UserRegistered(user: UserResponse.Root) extends ActivityBase[UserRegistered]

  case class UserActivated(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserActivated]

  case class UserBlacklisted(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserBlacklisted]

  case class UserRemovedFromBlacklist(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserRemovedFromBlacklist]

  case class UserEnabled(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserEnabled]

  case class UserDisabled(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserDisabled]

  case class UserRemindPassword(user: UserResponse.Root, code: String, isAdmin: Boolean)
      extends ActivityBase[UserRemindPassword]

  case class UserPasswordReset(user: UserResponse.Root) extends ActivityBase[UserPasswordReset]

  case class UserPasswordChanged(user: UserResponse.Root) extends ActivityBase[UserPasswordChanged]

  case class UserUpdated(oldInfo: UserResponse.Root,
                         newInfo: UserResponse.Root,
                         admin: Option[UserResponse.Root] = None)
      extends ActivityBase[UserUpdated]

  /* User Addresses */
  case class UserAddressCreated(user: UserResponse.Root,
                                address: AddressResponse,
                                admin: Option[UserResponse.Root])
      extends ActivityBase[UserAddressCreated]

  case class UserAddressUpdated(user: UserResponse.Root,
                                oldInfo: AddressResponse,
                                newInfo: AddressResponse,
                                admin: Option[UserResponse.Root])
      extends ActivityBase[UserAddressUpdated]

  case class UserAddressDeleted(user: UserResponse.Root,
                                address: AddressResponse,
                                admin: Option[UserResponse.Root])
      extends ActivityBase[UserAddressDeleted]

  /* User Credit Cards */
  case class CreditCardAdded(user: UserResponse.Root,
                             creditCard: CreditCardsResponse.RootSimple,
                             admin: Option[UserResponse.Root])
      extends ActivityBase[CreditCardAdded]

  case class CreditCardUpdated(user: UserResponse.Root,
                               oldInfo: CreditCardsResponse.RootSimple,
                               newInfo: CreditCardsResponse.RootSimple,
                               admin: Option[UserResponse.Root])
      extends ActivityBase[CreditCardUpdated]

  case class CreditCardRemoved(user: UserResponse.Root,
                               creditCard: CreditCardsResponse.RootSimple,
                               admin: Option[UserResponse.Root])
      extends ActivityBase[CreditCardRemoved]
}
