package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.payment.creditcard._
import utils.db._
import utils.seeds.generators.CreditCardGenerator

trait CreditCardSeeds extends CreditCardGenerator {

  val gatewayCustomerId = randomCustomerGatewayId

  def createCreditCards(customers: CustomerSeeds#CustomerIds): DbResultT[Unit] =
    for {
      _ ← * <~ CreditCards.createAll(
        Seq(creditCard1.copy(accountId = customers._1),
            creditCard2.copy(accountId = customers._1),
            creditCard3.copy(accountId = customers._2),
            creditCard4.copy(accountId = customers._3),
            creditCard5.copy(accountId = customers._4)))
    } yield {}

  def creditCard1 = {
    CreditCard(accountId = 0,
               gatewayCustomerId = gatewayCustomerId,
               gatewayCardId = "",
               holderName = "Yax",
               lastFour = "4242",
               expMonth = today.getMonthValue,
               expYear = today.getYear + 2,
               isDefault = true,
               address = BillingAddress(regionId = 4129,
                                        name = "Old Jeff",
                                        address1 = "95 W. 5th Ave.",
                                        address2 = Some("Apt. 437"),
                                        city = "San Mateo",
                                        zip = "94402"),
               brand = "Visa")
  }

  def creditCard = creditCard1

  def creditCard2 = {
    CreditCard(accountId = 0,
               gatewayCustomerId = gatewayCustomerId,
               gatewayCardId = "",
               holderName = "Yax",
               lastFour = "8752",
               expMonth = 4,
               expYear = today.getYear + 4,
               isDefault = false,
               address = BillingAddress(regionId = 4141,
                                        name = "West Ave",
                                        address1 = "3590 West Avenue",
                                        address2 = None,
                                        city = "Indianapolis",
                                        zip = "46201"),
               brand = "Visa")
  }

  def creditCard3 = {
    CreditCard(accountId = 0,
               gatewayCustomerId = gatewayCustomerId,
               gatewayCardId = "",
               holderName = "Adil",
               lastFour = "3436",
               expMonth = 7,
               expYear = today.getYear + 3,
               isDefault = true,
               address = BillingAddress(regionId = 4164,
                                        name = "Haymond Rocks",
                                        address1 = "3564 Haymond Rocks Road",
                                        address2 = None,
                                        city = "Grants Pass",
                                        zip = "97526"),
               brand = "Visa")
  }

  def creditCard4 = {
    CreditCard(accountId = 0,
               gatewayCustomerId = gatewayCustomerId,
               gatewayCardId = "",
               holderName = "John",
               lastFour = "3436",
               expMonth = 3,
               expYear = today.getYear + 3,
               isDefault = true,
               address = BillingAddress(regionId = 551,
                                        name = "Bright Quay",
                                        address1 = "1851 Bright Quay",
                                        address2 = None,
                                        city = "Tonganoxie",
                                        zip = "S0R-9U4"),
               brand = "Visa")
  }

  def creditCard5 = {
    CreditCard(accountId = 0,
               gatewayCustomerId = gatewayCustomerId,
               gatewayCardId = "",
               holderName = "František",
               lastFour = "1258",
               expMonth = 11,
               expYear = today.getYear + 1,
               isDefault = true,
               brand = "Visa",
               address = BillingAddress(regionId = 787,
                                        name = "Výchozí",
                                        address1 = "Rvačov 829",
                                        address2 = None,
                                        city = "Rvačov 829",
                                        zip = "413 01"))
  }
}
