package com.foxcommerce.payloads

final case class CustomerAddressPayload(name: String, regionId: Long, address1: String, address2: String,
  city: String, zip: String)