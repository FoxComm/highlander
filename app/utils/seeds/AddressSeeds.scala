package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.{Address, Addresses}
import utils.DbResultT.implicits._
import utils.DbResultT.{DbResultT, _}

trait AddressSeeds {

  def createAddresses(customers: CustomerSeeds#Customers): DbResultT[Unit] = for {
    _ ← * <~ Addresses.createAll(Seq(
      usAddress1.copy(customerId = customers._1),
      usAddress2.copy(customerId = customers._1),
      usAddress3.copy(customerId = customers._2),
      usAddress4.copy(customerId = customers._2),
      canadaAddress1.copy(customerId = customers._3),
      canadaAddress2.copy(customerId = customers._3),
      rowAddress1.copy(customerId = customers._4)
    ))
  } yield {}

  def usAddress1 = Address(customerId = 0, regionId = 4177, name = "Home", address1 = "555 E Lake Union St.",
    address2 = None, city = "Seattle", zip = "12345", isDefaultShipping = true, phoneNumber = None)

  def address = usAddress1

  def usAddress2 = Address(customerId = 0, regionId = 4165, name = "Other", address1 = "4749 Grove Avenue",
    address2 = None, city = "Camp Hill", zip = "17011", phoneNumber = None)

  def usAddress3 = Address(customerId = 0, regionId = 4154, name = "Temp", address1 = "3104 Canterbury Court",
    address2 = "★ ★ ★".some, city = "Cornelius", zip = "28031", phoneNumber = "2025550113".some)

  def usAddress4 = Address(customerId = 0, regionId = 4162, name = "The address", address1 = "3345 Orchard Lane",
    address2 = None, city = "Avon Lake", zip = "44012", phoneNumber = None, isDefaultShipping = true)

  def canadaAddress1 = Address(customerId = 0, regionId = 545, name = "Address 1", address1 = "4177 Crystal Downs",
    address2 = None, city = "Goodnews Bay", zip = "B9P-4U0", phoneNumber = "9024655753".some,
    isDefaultShipping = true)

  def canadaAddress2 = Address(customerId = 0, regionId = 547, name = "Address 2", address1 = "8321 Harvest Woods",
    address2 = None, city = "Ptarmigan", zip = "X9M-9W6", phoneNumber = "8671002677".some)

  def rowAddress1 = Address(customerId = 0, regionId = 789, name = "Dům", address1 = "Příční 151", address2 = None,
    city = "Bystricka", zip = "756 24", phoneNumber = "578660629".some, isDefaultShipping = true)
}
