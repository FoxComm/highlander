package models.traits

import models.customer.Customer
import models.StoreAdmin

sealed trait Originator

final case class CustomerOriginator(customer: Customer) extends Originator

final case class AdminOriginator(admin: StoreAdmin) extends Originator

object Originator {
  def apply(customer: Customer): CustomerOriginator = CustomerOriginator(customer)
  def apply(admin: StoreAdmin): AdminOriginator = AdminOriginator(admin)
}
