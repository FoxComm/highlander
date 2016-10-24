package utils.seeds.generators

import cats.implicits._
import faker._
import models.account.{Scope, User}
import utils.aliases.AU

trait CustomerGenerator {

  def generateCustomer(implicit au: AU): User = {
    val name = Name.name
    User(accountId = 0, email = generateEmail(name).some, name = name.some, scope = Scope.current)
  }

  def generateCustomers(total: Int)(implicit au: AU): Seq[User] =
    (1 to total) map { c ⇒
      generateCustomer
    }

  private def generateEmail(name: String): String = {
    val base = new Base {}
    val num  = base.bothify("??##")
    Internet.user_name(name) + num + "@" + Internet.domain_name
  }
}
