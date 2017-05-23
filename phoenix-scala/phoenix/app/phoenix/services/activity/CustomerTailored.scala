package phoenix.services.activity

import phoenix.responses.{AddressResponse, CreditCardsResponse, UserResponse, CustomerResponse}

object CustomerTailored {
  case class CustomerCreated(admin: UserResponse.Root, user: CustomerResponse.Root)
      extends ActivityBase[CustomerCreated]

  case class CustomerRegistered(user: CustomerResponse.Root)
      extends ActivityBase[CustomerRegistered]

  case class CustomerActivated(admin: UserResponse.Root, user: CustomerResponse.Root)
      extends ActivityBase[CustomerActivated]

  case class CustomerBlacklisted(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[CustomerBlacklisted]

  case class CustomerRemovedFromBlacklist(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[CustomerRemovedFromBlacklist]

  case class CustomerEnabled(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[CustomerEnabled]

  case class CustomerDisabled(admin: UserResponse.Root, user: UserResponse.Root)
      extends ActivityBase[CustomerDisabled]

  case class CustomerRemindPassword(user: UserResponse.Root, code: String)
      extends ActivityBase[CustomerRemindPassword]

  case class CustomerUpdated(oldInfo: UserResponse.Root,
                             newInfo: UserResponse.Root,
                             admin: Option[UserResponse.Root] = None)
      extends ActivityBase[CustomerUpdated]

  /* Customer Addresses */
  case class CustomerAddressCreated(user: UserResponse.Root,
                                    address: AddressResponse,
                                    admin: Option[UserResponse.Root])
      extends ActivityBase[CustomerAddressCreated]

  case class CustomerAddressUpdated(user: UserResponse.Root,
                                    oldInfo: AddressResponse,
                                    newInfo: AddressResponse,
                                    admin: Option[UserResponse.Root])
      extends ActivityBase[CustomerAddressUpdated]

  case class CustomerAddressDeleted(user: UserResponse.Root,
                                    address: AddressResponse,
                                    admin: Option[UserResponse.Root])
      extends ActivityBase[CustomerAddressDeleted]
}
