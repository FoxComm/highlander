package services.activity

import responses.{Addresses, CustomerResponse, CreditCardsResponse, StoreAdminResponse}

object CustomerTailored {
  case class CustomerCreated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerCreated]

  case class CustomerRegistered(customer: CustomerResponse.Root)
      extends ActivityBase[CustomerRegistered]

  case class CustomerActivated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerActivated]

  case class CustomerBlacklisted(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerBlacklisted]

  case class CustomerRemovedFromBlacklist(
      admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerRemovedFromBlacklist]

  case class CustomerEnabled(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerEnabled]

  case class CustomerDisabled(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
      extends ActivityBase[CustomerDisabled]

  case class CustomerUpdated(oldInfo: CustomerResponse.Root,
                             newInfo: CustomerResponse.Root,
                             admin: Option[StoreAdminResponse.Root] = None)
      extends ActivityBase[CustomerUpdated]

  /* Customer Addresses */
  case class CustomerAddressCreated(customer: CustomerResponse.Root,
                                    address: Addresses.Root,
                                    admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CustomerAddressCreated]

  case class CustomerAddressUpdated(customer: CustomerResponse.Root,
                                    oldInfo: Addresses.Root,
                                    newInfo: Addresses.Root,
                                    admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CustomerAddressUpdated]

  case class CustomerAddressDeleted(customer: CustomerResponse.Root,
                                    address: Addresses.Root,
                                    admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CustomerAddressDeleted]

  /* Customer Credit Cards */
  case class CreditCardAdded(customer: CustomerResponse.Root,
                             creditCard: CreditCardsResponse.RootSimple,
                             admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CreditCardAdded]

  case class CreditCardUpdated(customer: CustomerResponse.Root,
                               oldInfo: CreditCardsResponse.RootSimple,
                               newInfo: CreditCardsResponse.RootSimple,
                               admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CreditCardUpdated]

  case class CreditCardRemoved(customer: CustomerResponse.Root,
                               creditCard: CreditCardsResponse.RootSimple,
                               admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CreditCardRemoved]
}
