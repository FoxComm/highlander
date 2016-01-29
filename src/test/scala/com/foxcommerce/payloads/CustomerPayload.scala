package com.foxcommerce.payloads

final case class CustomerPayload(name: String, email: String, isBlacklisted: Boolean = false, isDisabled: Boolean =
  false, address: Option[CustomerAddressPayload] = None)