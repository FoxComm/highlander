package utils.seeds

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import com.github.tototoshi.csv._
import models.{StoreAdmin, StoreAdmins}
import utils.db._
import utils.db.DbResultT._

trait StoreAdminSeeds {

  def createStoreAdmins: DbResultT[StoreAdmin#Id] = {
    val reader = CSVReader.open(new File("gatling-classes/data/store_admins.csv"))
    val admins = reader.all.drop(1).collect {
      case name :: email :: password :: Nil ⇒
        StoreAdmin.build(name = name, email = email, password = Some(password))
    }
    reader.close()
    for {
      admins ← * <~ StoreAdmins.createAllReturningIds(admins)
    } yield admins.head
  }

  def storeAdmin = StoreAdmin.build(email = "admin@admin.com", password = "password".some, name = "Frankly Admin")

}
