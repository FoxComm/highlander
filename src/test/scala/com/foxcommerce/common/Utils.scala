package com.foxcommerce.common

import com.foxcommerce.payloads.AddressPayload

object Utils {
  val defaultDomain = "foxcommerce.com"

  def randomEmail(prefix: String, domain: String = defaultDomain): String = {
    prefix ++ "_" ++ randomString(10) ++ "@" ++ domain
  }

  def randomString(length: Int) = {
  	val rnd = new scala.util.Random()
  	rnd.nextString(length)
  }

  def addressPayloadBody(address: AddressPayload): String =
    """{"name": "%s", "regionId": %d, "address1": "%s", "address2": "%s", "city": "%s", "zip": "%s"}""".
      format(address.name, address.regionId, address.address1, address.address2, address.city, address.zip)
}