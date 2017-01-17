package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import models.account._
import models.{Note, Notes}
import payloads.CustomerPayloads.CreateCustomerPayload
import services.account._
import services.customers._
import utils.aliases._
import utils.db._

trait CustomerSeeds {

  type CustomerIds = (Account#Id, Account#Id, Account#Id, Account#Id)

  def createCustomer(user: User, isGuest: Boolean, scopeId: Int, password: Option[String] = None)(
      implicit db: DB,
      ac: AC): DbResultT[User] = {

    val payload = CreateCustomerPayload(email = user.email.getOrElse(""),
                                        name = user.name,
                                        password = password,
                                        isGuest = Some(isGuest))

    val createContext =
      AccountCreateContext(roles = List("customer"), org = "merchant", scopeId = scopeId)

    for {
      response ← * <~ CustomerManager.createFromAdmin(payload = payload, context = createContext)
      user     ← * <~ Users.mustFindByAccountId(response.id)
    } yield user
  }

  def createCustomers(scopeId: Int)(implicit db: DB, ac: AC): DbResultT[CustomerIds] =
    for {
      users ← * <~ customers.map(
        c ⇒
          createCustomer(user = c,
                         isGuest = c.accountId == 100,
                         scopeId = scopeId,
                         password = "password".some))
      accountIds = users.map(_.accountId)
      scope ← * <~ Scopes.mustFindById400(scopeId)
      _ ← * <~ Notes.createAll(
        customerNotes(LTree(scope.path)).map(_.copy(referenceId = accountIds.head)))
    } yield
      accountIds.toList match {
        case c1 :: c2 :: c3 :: c4 :: Nil ⇒ (c1, c2, c3, c4)
        case _                           ⇒ ???
      }

  def usCustomer1 =
    User(accountId = 0,
         name = "Yax Man".some,
         email = "yax@yax.com".some,
         phoneNumber = Some("123-444-4388"))

  def usCustomer2 =
    User(accountId = 0,
         email = "adil@adil.com".some,
         phoneNumber = "123-444-0909".some,
         isDisabled = true) // FIXME: `disabledBy` is not required for `isDisabled`=true

  def canadaCustomer =
    User(accountId = 100, //hack to make one guest
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

  def customerNotes(scope: LTree): Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1,
           referenceType = Note.Customer,
           storeAdminId = 1,
           body = body,
           scope = scope)
    Seq(
      newNote("This customer is a donkey."),
      newNote("No, seriously."),
      newNote("Like, an actual donkey."),
      newNote("How did a donkey even place an order on our website?")
    )
  }
}
