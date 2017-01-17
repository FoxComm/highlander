package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.location.{Address, Addresses}
import utils.db._

trait AddressSeeds {

  def createAddresses(customers: CustomerSeeds#CustomerIds): DbResultT[Unit] =
    for {
      _ ← * <~ Addresses.createAll(
        Seq(
          usAddress1.copy(accountId = customers._1),
          usAddress2.copy(accountId = customers._1),
          usAddress3.copy(accountId = customers._2),
          usAddress4.copy(accountId = customers._2),
          canadaAddress1.copy(accountId = customers._3),
          canadaAddress2.copy(accountId = customers._3),
          rowAddress1.copy(accountId = customers._4)
        ))
    } yield {}

  def usAddress1 =
    Address(accountId = 0,
            regionId = 4177,
            name = "Curt Cobain",
            address1 = "555 E Lake Union St.",
            address2 = None,
            city = "Seattle",
            zip = "12345",
            isDefaultShipping = true,
            phoneNumber = None)

  def address = usAddress1

  def usAddress2 =
    Address(accountId = 0,
            regionId = 4165,
            name = "Freddie Mercury",
            address1 = "4749 Grove Avenue",
            address2 = None,
            city = "Camp Hill",
            zip = "17011",
            phoneNumber = None)

  def usAddress3 =
    Address(accountId = 0,
            regionId = 4154,
            name = "Mark Sandman",
            address1 = "3104 Canterbury Court",
            address2 = "Morphine".some,
            city = "Cornelius",
            zip = "28031",
            phoneNumber = "2025550113".some)

  def usAddress4 =
    Address(accountId = 0,
            regionId = 4162,
            name = "Jim Morrison",
            address1 = "3345 Orchard Lane",
            address2 = None,
            city = "Avon Lake",
            zip = "44012",
            phoneNumber = None,
            isDefaultShipping = true)

  def canadaAddress1 =
    Address(accountId = 0,
            regionId = 545,
            name = "Ozzy Osbourne",
            address1 = "4177 Crystal Downs",
            address2 = None,
            city = "Goodnews Bay",
            zip = "B9P-4U0",
            phoneNumber = "9024655753".some,
            isDefaultShipping = true)

  def canadaAddress2 =
    Address(accountId = 0,
            regionId = 547,
            name = "Stevie Wonder",
            address1 = "8321 Harvest Woods",
            address2 = None,
            city = "Ptarmigan",
            zip = "X9M-9W6",
            phoneNumber = "8671002677".some)

  def rowAddress1 =
    Address(accountId = 0,
            regionId = 789,
            name = "Amy Winehouse",
            address1 = "Příční 151",
            address2 = None,
            city = "Bystricka",
            zip = "756 24",
            phoneNumber = "578660629".some,
            isDefaultShipping = true)
}
