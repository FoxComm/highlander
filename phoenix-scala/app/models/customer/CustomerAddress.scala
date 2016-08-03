package models.customer

import models.location

case class CustomerAddress(customerId: Int)
    extends Address {}

object CustomerAddress
    extends Address {}

class CustomerAddresses(tag: Tag) extends Addresses[CustomerAddress](tag, "customerAddresses") {
  def customerId      =  column[Int]("customer_id")

  def * = (id)
}
