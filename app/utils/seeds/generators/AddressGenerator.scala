package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import models.location.Address
import models.customer.Customer
import utils.db._
import utils.db.DbResultT._
import GeneratorUtils.randomString

import faker._;

trait AddressGenerator {

  //TODO
  //
  //This is a really dumb random address generator. None of the addresses are valid.
  //TODO: Generate random geo coordinates from distance of random city in a set of cities.
  //      Use reverse geo coder to get address of coordinate.
  //      This can probably be done once offline and stored in a file and pulled here.
  def generateAddress(customer: Customer, isDefault: Boolean): Address = {
    val base = new Base{}
    
    val houseNumberTemplate = base.fetch[String]("address.street_address")
    val houseNumber: String  = base.numerify(houseNumberTemplate)
    val streetName: String  = Name.last_name
    val address1 = s"$houseNumber $streetName"

    val hasAddress2 = Random.nextBoolean()
    
    val apartment = Name.numerify(Name.fetch("address.secondary_address"))
    val address2 = if(hasAddress2) Some(apartment) else None

    val citySuffix = Name.fetch[String]("address.street_suffix")
    val city = s"${Name.first_name} $citySuffix"
    val zip = Name.numerify("#####")
    val hasPhone = Random.nextBoolean()
    val phoneNumber = if(hasPhone) Some(Name.numerify("##########")) else None

    Address(customerId = customer.id, regionId = 4177, name = customer.name.getOrElse(Name.name), 
      address1 = streetName, address2 = address2, city = city, zip = zip, 
      isDefaultShipping = isDefault, phoneNumber = phoneNumber)
  }
}
