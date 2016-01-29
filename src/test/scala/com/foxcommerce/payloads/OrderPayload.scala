package com.foxcommerce.payloads

final case class OrderPayload(customer: CustomerPayload, shippingAddress: AddressPayload)