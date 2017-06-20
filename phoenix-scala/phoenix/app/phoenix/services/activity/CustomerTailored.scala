package phoenix.services.activity

import phoenix.responses.AddressResponse
import phoenix.responses.users.{CustomerResponse, UserResponse}

object CustomerTailored {
  case class CustomerCreated(admin: UserResponse, user: CustomerResponse)
      extends ActivityBase[CustomerCreated]

  case class CustomerRegistered(user: CustomerResponse) extends ActivityBase[CustomerRegistered]

  case class CustomerActivated(admin: UserResponse, user: CustomerResponse)
      extends ActivityBase[CustomerActivated]

  case class CustomerBlacklisted(admin: UserResponse, user: UserResponse)
      extends ActivityBase[CustomerBlacklisted]

  case class CustomerRemovedFromBlacklist(admin: UserResponse, user: UserResponse)
      extends ActivityBase[CustomerRemovedFromBlacklist]

  case class CustomerEnabled(admin: UserResponse, user: UserResponse) extends ActivityBase[CustomerEnabled]

  case class CustomerDisabled(admin: UserResponse, user: UserResponse) extends ActivityBase[CustomerDisabled]

  case class CustomerRemindPassword(user: UserResponse, code: String)
      extends ActivityBase[CustomerRemindPassword]

  case class CustomerUpdated(oldInfo: UserResponse, newInfo: UserResponse, admin: Option[UserResponse] = None)
      extends ActivityBase[CustomerUpdated]

  /* Customer Addresses */
  case class CustomerAddressCreated(user: UserResponse, address: AddressResponse, admin: Option[UserResponse])
      extends ActivityBase[CustomerAddressCreated]

  case class CustomerAddressUpdated(user: UserResponse,
                                    oldInfo: AddressResponse,
                                    newInfo: AddressResponse,
                                    admin: Option[UserResponse])
      extends ActivityBase[CustomerAddressUpdated]

  case class CustomerAddressDeleted(user: UserResponse, address: AddressResponse, admin: Option[UserResponse])
      extends ActivityBase[CustomerAddressDeleted]
}
