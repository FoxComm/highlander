package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.account._
import models.customer._
import models.{Note, Notes}
import utils.db._

trait CustomerSeeds {

  type CustomerIds = (Account#Id, Account#Id, Account#Id, Account#Id)

  def createCustomers: DbResultT[CustomerIds] =
    for {
      accountIds ← * <~ Accounts.createAllReturningIds(customers.map(_ ⇒ Account()))
      accountCustomers = accountIds zip customers
      customerIds ← * <~ Users.createAllReturningIds(accountCustomers.map {
                     case (accountId, customers) ⇒
                       customer.copy(accountId = accountId)
                   })
      ids = accountIds zip customerIds
      _ ← * <~ CustomerUsers.createAllReturningIds(ids.map {
           case (accountId, userId) ⇒
             CustomerUser(accountId = accountId, userId = userId, isGuest = accountId == 3)
         })
      _ ← * <~ Notes.createAll(customerNotes.map(_.copy(referenceId = accountIds.head)))
    } yield
      accountIds.toList match {
        case c1 :: c2 :: c3 :: c4 :: Nil ⇒ (c1, c2, c3, c4)
        case _                           ⇒ ???
      }

  def usCustomer1 =
    User(accountId = 0, email = "yax@yax.com".some, phoneNumber = Some("123-444-4388"))

  def usCustomer2 =
    User(accountId = 0,
         email = "adil@adil.com".some,
         phoneNumber = "123-444-0909".some,
         isDisabled = true) // FIXME: `disabledBy` is not required for `isDisabled`=true

  def canadaCustomer =
    User(accountId = 0,
         email = "iamvery@sorry.com".some,
         name = "John Nicholson".some,
         phoneNumber = Some("858-867-5309"))

  def rowCustomer =
    User(accountId = 0,
         email = "fran@absinthelovers.cz".some,
         name = "František Materna".some,
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
