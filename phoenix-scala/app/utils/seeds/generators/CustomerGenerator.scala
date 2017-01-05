package utils.seeds.generators

import models.account.User

import cats.implicits._
import faker._

trait CustomerGenerator {

  def generateCustomer: User = {
    val name = Name.name
    User(accountId = 0, email = generateEmail(name).some, name = name.some)
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
