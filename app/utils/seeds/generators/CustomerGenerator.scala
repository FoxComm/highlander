package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customer
import GeneratorUtils.randomString

import cats.implicits._
import faker._

trait CustomerGenerator {

  def generateCustomer(location: String): Customer = { 
    val name = Name.name
    Customer.build(email = generateEmail(name), password = randomString(10).some,
      name = name.some, location = location.some)
  }

  def generateCustomers(total: Int, location: String) : Seq[Customer] =
    (1 to total) map { c ⇒  generateCustomer(location)}

  private def generateEmail(name: String) : String = {
    val base = new Base{}
    val num = base.bothify("??##")
    Internet.user_name(name) + num + "@" + Internet.domain_name
  }
}
