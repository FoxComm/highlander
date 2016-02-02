package com.foxcommerce.fixtures

final case class CustomerFixture(name: String, emailPrefix: String, address: AddressFixture,
  storeCreditCount: Long = 0, storeCreditTotal: Long = 0, isBlacklisted: Boolean = false, isDisabled: Boolean = false)