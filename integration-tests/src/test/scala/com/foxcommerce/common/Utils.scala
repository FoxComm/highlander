package com.foxcommerce.common

import java.util.concurrent.ThreadLocalRandom

import com.foxcommerce.fixtures.{CreditCardFixture, AddressFixture}

object Utils {
  val defaultDomain = "foxcommerce.com"

  def randomEmail(prefix: String, domain: String = defaultDomain): String = {
    prefix ++ "_" ++ randomString() ++ "@" ++ domain
  }

  def randomString(limit: Int = 1000000) = ThreadLocalRandom.current.nextInt(limit).toString

  def addressPayloadBody(address: AddressFixture): String =
    """{"name": "%s", "regionId": %d, "address1": "%s", "address2": "%s", "city": "%s", "zip": "%s"}""".
      format(address.name, address.regionId, address.address1, address.address2, address.city, address.zip)
}