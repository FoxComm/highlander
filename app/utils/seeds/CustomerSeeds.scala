package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.customer.{Customer, Customers}
import models.{Note, Notes}
import utils.db._

trait CustomerSeeds {

  type Customers = (Customer#Id, Customer#Id, Customer#Id, Customer#Id)

  def createCustomers: DbResultT[Customers] =
    for {
      customers ← * <~ Customers.createAllReturningIds(customers)
      _         ← * <~ Notes.createAll(customerNotes.map(_.copy(referenceId = customers.head)))
    } yield
      customers.toList match {
        case c1 :: c2 :: c3 :: c4 :: Nil ⇒ (c1, c2, c3, c4)
        case _                           ⇒ ???
      }

  def usCustomer1 =
    Customer.build(email = "yax@yax.com",
                   password = "password".some,
                   name = Some("Yax Fuentes"),
                   phoneNumber = Some("123-444-4388"),
                   location = Some("DonkeyVille, TN"),
                   modality = Some("Desktop"))

  def usCustomer2 =
    Customer.build(email = "adil@adil.com",
                   password = "password".some,
                   name = Some("Adil Wali"),
                   phoneNumber = Some("123-444-0909"),
                   location = Some("DonkeyHill, WA"),
                   modality = Some("Desktop"),
                   isDisabled = true) // FIXME: `disabledBy` is not required for `isDisabled`=true

  def canadaCustomer =
    Customer.build(email = "iamvery@sorry.com",
                   password = "password".some,
                   name = Some("John Nicholson"),
                   phoneNumber = Some("858-867-5309"),
                   location = Some("Donkeyburg, Canada"),
                   modality = Some("Tablet"),
                   isGuest = true)

  def rowCustomer =
    Customer.build(email = "fran@absinthelovers.cz",
                   password = "password".some,
                   name = Some("František Materna"),
                   phoneNumber = Some("883-444-4321"),
                   location = Some("Ďónkov, Czech Republic"),
                   modality = Some("Phone"))

  def customers: Seq[Customer] = Seq(usCustomer1, usCustomer2, canadaCustomer, rowCustomer)

  def customer: Customer = usCustomer1

  def customerNotes: Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1, referenceType = Note.Customer, storeAdminId = 1, body = body)
    Seq(
        newNote("This customer is a donkey."),
        newNote("No, seriously."),
        newNote("Like, an actual donkey."),
        newNote("How did a donkey even place an order on our website?")
    )
  }
}
