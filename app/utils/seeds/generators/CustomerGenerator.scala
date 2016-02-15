package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customer
import GeneratorUtils.randomString

import faker._;

trait CustomerGenerator {

  def generateCustomer(location: String): Customer = Customer(email = Internet.email,
    password = Some(randomString(10)), name = Some(Name.name), location=Some(location))

  def generateCustomers(total: Int, location: String) : Seq[Customer] =
    (1 to total) map { c ⇒  generateCustomer(location)}
}
