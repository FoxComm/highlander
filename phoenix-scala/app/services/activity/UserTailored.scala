package services.activity

import responses.{AddressResponse, CreditCardsResponse, UserResponse, StoreAdminResponse}

object UserTailored {
  case class UserCreated(creator: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserCreated]

  case class UserRegistered(user: UserResponse.Root) extends ActivityBase[UserRegistered]

  case class UserActivated(admin: StoreAdminResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserActivated]

  case class UserBlacklisted(admin: StoreAdminResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserBlacklisted]

  case class UserRemovedFromBlacklist(admin: StoreAdminResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserRemovedFromBlacklist]

  case class UserEnabled(admin: StoreAdminResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserEnabled]

  case class UserDisabled(admin: StoreAdminResponse.Root, user: UserResponse.Root)
      extends ActivityBase[UserDisabled]

  case class UserRemindPassword(user: UserResponse.Root, code: String)
      extends ActivityBase[UserRemindPassword]

  case class UserPasswordReset(user: UserResponse.Root) extends ActivityBase[UserPasswordReset]

  case class UserUpdated(oldInfo: UserResponse.Root,
                         newInfo: UserResponse.Root,
                         admin: Option[StoreAdminResponse.Root] = None)
      extends ActivityBase[UserUpdated]

  /* User Addresses */
  case class UserAddressCreated(user: UserResponse.Root,
                                address: AddressResponse,
                                admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[UserAddressCreated]

  case class UserAddressUpdated(user: UserResponse.Root,
                                oldInfo: AddressResponse,
                                newInfo: AddressResponse,
                                admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[UserAddressUpdated]

  case class UserAddressDeleted(user: UserResponse.Root,
                                address: AddressResponse,
                                admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[UserAddressDeleted]

  /* User Credit Cards */
  case class CreditCardAdded(user: UserResponse.Root,
                             creditCard: CreditCardsResponse.RootSimple,
                             admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CreditCardAdded]

  case class CreditCardUpdated(user: UserResponse.Root,
                               oldInfo: CreditCardsResponse.RootSimple,
                               newInfo: CreditCardsResponse.RootSimple,
                               admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CreditCardUpdated]

  case class CreditCardRemoved(user: UserResponse.Root,
                               creditCard: CreditCardsResponse.RootSimple,
                               admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CreditCardRemoved]
}
