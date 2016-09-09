package utils.seeds.generators

import models.account.User
import GeneratorUtils.randomString
import utils.Passwords.hashPassword

import cats.implicits._
import faker._

trait CustomerGenerator {

  def generateCustomer:  User = {
    val name = Name.name
    User(email = generateEmail(name).some,
             name = name.some)
  }

  def generateCustomers(total: Int): Seq[User] =
    (1 to total) map { c ⇒
      generateCustomer
    }

  private def generateEmail(name: String): String = {
    val base = new Base {}
    val num  = base.bothify("??##")
    Internet.user_name(name) + num + "@" + Internet.domain_name
  }
}
