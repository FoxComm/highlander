package utils.seeds

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import com.github.tototoshi.csv._
import models.account._
import models.admin._
import services.StoreAdminManager
import failures.UserFailures._
import utils.db._
import utils.aliases._

import payloads.StoreAdminPayloads.CreateStoreAdminPayload

trait StoreAdminSeeds {

  def createStoreAdmin(user: User,
                       password: String,
                       org: String,
                       roles: List[String],
                       state: AdminData.State,
                       author: Option[User])(implicit ec: EC, db: DB, ac: AC) = {

    val payload = CreateStoreAdminPayload(email = user.email.getOrElse(""),
                                          name = user.name.getOrElse(""),
                                          phoneNumber = user.phoneNumber,
                                          password = password.some,
                                          roles = roles,
                                          org = org)

    for {
      response  ← * <~ StoreAdminManager.create(payload = payload, author = author)
      user      ← * <~ Users.mustFindByAccountId(response.id)
      adminUser ← * <~ AdminsData.mustFindByAccountId(user.accountId)
      _         ← * <~ AdminsData.update(adminUser, adminUser.copy(state = state))
    } yield user
  }

  def createStoreAdmins(implicit ec: EC, db: DB, ac: AC): DbResultT[Int] = {
    val reader = CSVReader.open(new File("gatling-classes/data/store_admins.csv"))
    val admins = reader.all.drop(1).collect {
      case name :: email :: password :: org :: role :: Nil ⇒ {
        val user = User(accountId = 0, name = name.some, email = email.some)
        createStoreAdmin(user, password, org, List(role), AdminData.Active, None)
      }
    }
    reader.close()
    for {
      admins ← * <~ DbResultT.seqCollectFailures(admins)
    } yield admins.head.accountId
  }

  def createStoreAdminManual(username: String, email: String, org: String, roles: List[String])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[User] = {
    val pw_env = sys.props.get("admin_password").orElse(sys.env.get("ADMIN_PASSWORD"))
    val pw = pw_env match {
      case Some(p) ⇒ p
      case None    ⇒ scala.io.StdIn.readLine(s"Enter password for new admin $username: ")
    }

    val user = User(accountId = 0, name = username.some, email = email.some)
    createStoreAdmin(user, pw, org, roles, AdminData.Active, None)
  }

  def storeAdmin = User(accountId = 0, name = "admin".some, email = "admin@admin.com".some)
}
