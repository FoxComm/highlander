package services.activity

import responses.{Addresses, CustomerResponse, CreditCardsResponse, StoreAdminResponse}

object CustomerTailored {
  final case class CustomerCreated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
    extends ActivityBase[CustomerCreated]

  final case class CustomerRegistered(customer: CustomerResponse.Root)
    extends ActivityBase[CustomerRegistered]

  final case class CustomerActivated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
    extends ActivityBase[CustomerActivated]

  final case class CustomerBlacklisted(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
    extends ActivityBase[CustomerBlacklisted]

  final case class CustomerRemovedFromBlacklist(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
    extends ActivityBase[CustomerRemovedFromBlacklist]

  final case class CustomerEnabled(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
    extends ActivityBase[CustomerEnabled]

  final case class CustomerDisabled(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root)
    extends ActivityBase[CustomerDisabled]

  final case class CustomerUpdated(oldInfo: CustomerResponse.Root, newInfo: CustomerResponse.Root,
    admin: Option[StoreAdminResponse.Root] = None)
    extends ActivityBase[CustomerUpdated]

  /* Customer Addresses */
  final case class CustomerAddressCreated(customer: CustomerResponse.Root, address: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[CustomerAddressCreated]

  final case class CustomerAddressUpdated(customer: CustomerResponse.Root, oldInfo: Addresses.Root,
    newInfo: Addresses.Root, admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[CustomerAddressUpdated]

  final case class CustomerAddressDeleted(customer: CustomerResponse.Root, address: Addresses.Root,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[CustomerAddressDeleted]

  /* Customer Credit Cards */
  final case class CreditCardAdded(customer: CustomerResponse.Root, creditCard: CreditCardsResponse.RootSimple,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[CreditCardAdded]

  final case class CreditCardUpdated(customer: CustomerResponse.Root, oldInfo: CreditCardsResponse.RootSimple,
    newInfo: CreditCardsResponse.RootSimple, admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[CreditCardUpdated]

  final case class CreditCardRemoved(customer: CustomerResponse.Root, creditCard: CreditCardsResponse.RootSimple,
    admin: Option[StoreAdminResponse.Root])
    extends ActivityBase[CreditCardRemoved]
}
