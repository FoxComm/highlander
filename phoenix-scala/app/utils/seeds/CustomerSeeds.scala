package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.account._
import models.{Note, Notes}
import utils.db._

trait CustomerSeeds {

  type CustomerIds = (Account#Id, Account#Id, Account#Id, Account#Id)

  def createCustomers: DbResultT[CustomerIds] =
    for {
      accountIds ← * <~ Accounts.createAllReturningIds(customers.map { Account())}
      accountCustomers = acountIds zip customer
      customerIds ← * <~ Users.createAllReturningIds(accountCustomers.map {
        (accountId, customer) ⇒ customer.copy(accountId = accountId)
      })
      ids = accountIds zip customerIds
      _ ← * <~ CustomerUsers.createAllReturningIds(ids.map { 
        (accountId, userId) ⇒  
          CustomerUser(accountId = accountId, userId = userId, isGuest = false)
      })
      _         ← * <~ Notes.createAll(customerNotes.map(_.copy(referenceId = accountIds.head)))
    } yield
      accountIds.toList match {
        case c1 :: c2 :: c3 :: c4 :: Nil ⇒ (c1, c2, c3, c4)
        case _                           ⇒ ???
      }

  def usCustomer1 =
    User.build(email = "yax@yax.com",
                   phoneNumber = Some("123-444-4388"))

  def usCustomer2 =
    User.build(email = "adil@adil.com",
                   phoneNumber = Some("123-444-0909"),
                   isDisabled = true) // FIXME: `disabledBy` is not required for `isDisabled`=true

  def canadaCustomer =
    User.build(email = "iamvery@sorry.com",
                   name = Some("John Nicholson"),
                   phoneNumber = Some("858-867-5309"),
                   isGuest = true)

  def rowCustomer =
    User.build(email = "fran@absinthelovers.cz",
                   name = Some("František Materna"),
                   phoneNumber = Some("883-444-4321"))

  def customers: Seq[User] = Seq(usCustomer1, usCustomer2, canadaCustomer, rowCustomer)

  def customer: User = usCustomer1

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
