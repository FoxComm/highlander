package com.foxcommerce.common

object Utils {
  val defaultDomain = "foxcommerce.com"

  def randomEmail(prefix: String, domain: String = defaultDomain): String = {
    prefix ++ "_" ++ randomString(10) ++ "@" ++ domain
  }

  def randomString(length: Int) = scala.util.Random.alphanumeric.take(length).mkString
}