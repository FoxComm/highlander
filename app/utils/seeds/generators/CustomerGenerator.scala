package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customer
import GeneratorUtils.randomString
import utils.Passwords.hashPassword

import cats.implicits._
import faker._

trait CustomerGenerator {

  val hashedPassword = hashPassword(randomString(10))

  def generateCustomer(location: String): Customer = { 
    val name = Name.name
    Customer(email = generateEmail(name), hashedPassword = hashedPassword.some,
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
