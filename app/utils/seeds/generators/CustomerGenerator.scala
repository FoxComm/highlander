package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customer
import GeneratorUtils.randomString

import faker._;

trait CustomerGenerator {

  def generateEmail(name:String) = {
    val base = new Base{}
    val num = base.bothify("??##")
    Internet.user_name(name) + num  + "@" + Internet.domain_name
  }

  def generateCustomer(location: String): Customer = { 
    val name = Name.name
    Customer(email = generateEmail(name), password = Some(randomString(10)), 
      name = Some(name), location=Some(location))
  }

  def generateCustomers(total: Int, location: String) : Seq[Customer] =
    (1 to total) map { c ⇒  generateCustomer(location)}
}
