package utils.seeds

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import com.github.tototoshi.csv._
import models.{StoreAdmin, StoreAdmins}
import utils.db._

trait StoreAdminSeeds {

  def createStoreAdmins: DbResultT[StoreAdmin#Id] = {
    val reader = CSVReader.open(new File("gatling-classes/data/store_admins.csv"))
    val admins = reader.all.drop(1).collect {
      case name :: email :: password :: Nil ⇒
        StoreAdmin
          .build(name = name, email = email, password = Some(password), state = StoreAdmin.Active)
    }
    reader.close()
    for {
      admins ← * <~ StoreAdmins.createAllReturningIds(admins)
    } yield admins.head
  }

  def createStoreAdminManual(username: String, email: String): DbResultT[StoreAdmin] = {
    val pw_env = sys.props.get("admin_password").orElse(sys.env.get("ADMIN_PASSWORD"))
    val pw = pw_env match {
      case Some(p) ⇒ p
      case None    ⇒ scala.io.StdIn.readLine(s"Enter password for new admin $username: ")
    }

    val admin = StoreAdmin
      .build(name = username, email = email, password = Some(pw), state = StoreAdmin.Active)
    StoreAdmins.create(admin)
  }

  def storeAdmin =
    StoreAdmin.build(email = "admin@admin.com",
                     password = "password".some,
                     name = "Frankly Admin",
                     state = StoreAdmin.Active)
}
