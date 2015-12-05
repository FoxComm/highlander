package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Customer, Customers}
import utils.DbResultT._
import utils.DbResultT.implicits._

trait CustomerSeeds {

  type Customers = (Customer#Id, Customer#Id, Customer#Id, Customer#Id)

  def createCustomers: DbResultT[Customers] = for {
    customers ← * <~ Customers.createAllReturningIds(customers)
  } yield customers.toList match {
      case c1 :: c2 :: c3 :: c4 :: Nil ⇒ (c1, c2, c3, c4)
      case _ ⇒ ???
    }

  def usCustomer1 = Customer(email = "yax@yax.com", password = Some("password"),
    name = Some("Yax Fuentes"), phoneNumber = Some("123-444-4388"),
    location = Some("DonkeyVille, TN"), modality = Some("Desktop"))

  def usCustomer2 = Customer(email = "adil@adil.com", password = Some("password"),
    name = Some("Adil Wali"), phoneNumber = Some("123-444-0909"),
    location = Some("DonkeyHill, WA"), modality = Some("Desktop"),
    isDisabled = true) // FIXME: `disabledBy` is not required for `isDisabled`=true

  def canadaCustomer = Customer(email = "iamvery@sorry.com", password = Some("password"),
    name = Some("John Nicholson"), phoneNumber = Some("858-867-5309"),
    location = Some("Donkeyburg, Canada"), modality = Some("Tablet"),
    isGuest = true)

  def rowCustomer = Customer(email = "fran@absinthelovers.cz", password = Some("password"),
    name = Some("František Materna"), phoneNumber = Some("883-444-4321"),
    location = Some("Ďónkov, Czech Republic"), modality = Some("Phone"))

  def customers: Seq[Customer] = Seq(usCustomer1, usCustomer2, canadaCustomer, rowCustomer)

  def customer: Customer = usCustomer1

}
