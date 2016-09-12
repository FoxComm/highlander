package utils.seeds

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import com.github.tototoshi.csv._
import models.account._
import utils.db._

trait StoreAdminSeeds {

  def createStoreAdmin(user: User, password: String, state: StoreAdminUser.State) =
    for {
      account ← * <~ Accounts.create(Account())
      accessMethod ← * <~ AccountAccessMethods.create(
                        AccountAccessMethod.build(account.id, "login", password))
      newUser ← * <~ Users.create(user.copy(accountId = account.id))
      _ ← * <~ StoreAdminUsers.create(
             StoreAdminUser(accountId = account.id, userId = newUser.id, state = state))
      //MAXDO Assign admin role
    } yield newUSer

  def createStoreAdmins: DbResultT[User#Id] = {
    val reader = CSVReader.open(new File("gatling-classes/data/store_admins.csv"))
    val admins = reader.all.drop(1).collect {
      case name :: email :: password :: Nil ⇒ {
        val user = User(name = name, email = email)
        createStoreAdmin(user, password, StoreAdminUser.Active)
      }
    }
    reader.close()
    for {
      admins ← * <~ DbResultT.sequence(admins)
    } yield admins.head.accountId
  }

  def createStoreAdminManual(username: String, email: String): DbResultT[User] = {
    val pw_env = sys.props.get("admin_password").orElse(sys.env.get("ADMIN_PASSWORD"))
    val pw = pw_env match {
      case Some(p) ⇒ p
      case None    ⇒ scala.io.StdIn.readLine(s"Enter password for new admin $username: ")
    }

    val user = User(name = name, email = email)
    createStoreAdmin(user, pw, StoreAdminUser.Active)
  }

  def storeAdmin =
    User(email = "admin@admin.com", name = "Frankly Admin")
}
