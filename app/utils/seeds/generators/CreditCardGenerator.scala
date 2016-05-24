package utils.seeds.generators

import java.time.{Instant, ZoneId}

import scala.concurrent.ExecutionContext.Implicits.global
import models.payment.creditcard.CreditCard
import models.customer.Customer
import utils.db._
import utils.db.DbResultT._
import GeneratorUtils.randomString

import scala.util.Random
import faker._;

trait CreditCardGenerator extends AddressGenerator {

  val gatewayCustomerIds = Seq("cus_8BV45FaedrbyMI",
                               "cus_8BCn3Ed2Yy1cd3",
                               "cus_8BACO27aB3l9Sc",
                               "cus_8AlvEEj9TKTG9j",
                               "cus_8AkS76A3bahhVg",
                               "cus_8AkOXndcxvvE0g",
                               "cus_8AjhoAuAqC6FWa",
                               "cus_8AjaWtPNbWQvqj",
                               "cus_8Aif0ZvXxMO7fd",
                               "cus_8Ai76BTTFay94J",
                               "cus_8AganQYh9ACK7C",
                               "cus_8Afrqouoyj7aDF",
                               "cus_887JE4o0xdvCuy",
                               "cus_83RUoL0ym7d4Mr",
                               "cus_83MgKmTYukiQQz",
                               "cus_83MZpoQsbtY0kK",
                               "cus_83LaOiTle6avEe",
                               "cus_83LWLy4ahFgRC9",
                               "cus_7zwyNO5QHlyqQd",
                               "cus_7ynmhqyqvd3HZc",
                               "cus_7yTU1j2Nvo1ifH",
                               "cus_7yTU0ENGfk0pxN",
                               "cus_7yTUzhYqMEwy1X")

  def randomCustomerGatewayId = gatewayCustomerIds(Random.nextInt(gatewayCustomerIds.length))

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  def generateCreditCard(customer: Customer) = {
    val base              = new Base {}
    val address           = generateAddress(customer, false)
    val gatewayCustomerId = randomCustomerGatewayId
    val gatewayCardId     = ""
    CreditCard(customerId = customer.id,
               gatewayCustomerId = gatewayCustomerId,
               gatewayCardId = gatewayCardId,
               holderName = customer.name.getOrElse(Name.name),
               lastFour = base.numerify("####"),
               expMonth = today.getMonthValue,
               expYear = today.getYear + 2,
               isDefault = true,
               regionId = 4129,
               addressName = address.name,
               address1 = address.address1,
               address2 = address.address2,
               city = address.city,
               zip = address.zip,
               brand = "Visa")
  }

  def generateCreditCards(customers: Seq[Customer]): Seq[CreditCard] =
    customers map generateCreditCard
}
