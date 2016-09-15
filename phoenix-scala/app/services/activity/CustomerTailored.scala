package services.activity

import responses.{AddressResponse, CreditCardsResponse, UserResponse, CustomerResponse}

object CustomerTailored {
  case class CustomerCreated(admin: UserResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerCreated]

  case class CustomerRegistered(customer: CustomerResponse.Root)
      extends ActivityBase[CustomerRegistered]

  case class CustomerActivated(admin: UserResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerActivated]

  case class CustomerBlacklisted(admin: UserResponse.Root, customer: UserResponse.Root)
      extends ActivityBase[CustomerBlacklisted]

  case class CustomerRemovedFromBlacklist(admin: UserResponse.Root, customer: UserResponse.Root)
      extends ActivityBase[CustomerRemovedFromBlacklist]

  case class CustomerEnabled(admin: UserResponse.Root, customer: UserResponse.Root)
      extends ActivityBase[CustomerEnabled]

  case class CustomerDisabled(admin: UserResponse.Root, customer: UserResponse.Root)
      extends ActivityBase[CustomerDisabled]

  case class CustomerRemindPassword(customer: UserResponse.Root, code: String)
      extends ActivityBase[CustomerRemindPassword]

  case class CustomerUpdated(oldInfo: UserResponse.Root,
                             newInfo: UserResponse.Root,
                             admin: Option[UserResponse.Root] = None)
      extends ActivityBase[CustomerUpdated]

  /* Customer Addresses */
  case class CustomerAddressCreated(customer: UserResponse.Root,
                                    address: AddressResponse,
                                    admin: Option[UserResponse.Root])
      extends ActivityBase[CustomerAddressCreated]

  case class CustomerAddressUpdated(customer: UserResponse.Root,
                                    oldInfo: AddressResponse,
                                    newInfo: AddressResponse,
                                    admin: Option[UserResponse.Root])
      extends ActivityBase[CustomerAddressUpdated]

  case class CustomerAddressDeleted(customer: UserResponse.Root,
                                    address: AddressResponse,
                                    admin: Option[UserResponse.Root])
      extends ActivityBase[CustomerAddressDeleted]
}
