package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.{StoreAdmin, StoreAdmins}
import utils.DbResultT.implicits._
import utils.DbResultT.{DbResultT, _}
import utils.Passwords.hashPassword

trait StoreAdminSeeds {

  // Returns only first admin id
  def createStoreAdmins: DbResultT[StoreAdmin#Id] = for {
    adminIds ← * <~ StoreAdmins.createAllReturningIds(storeAdmins)
  } yield adminIds.head

  def storeAdmins = Seq(
    StoreAdmin(email = "admin@admin.com", password = hashPassword("password"), name = "Frankly Admin"),
    StoreAdmin(email = "hackerman@yahoo.com", password = hashPassword("password1"), name = "Such Root"),
    StoreAdmin(email = "admin_hero@xakep.ru", password = hashPassword("password2"), name = "Admin Hero")
  )

  def storeAdmin = storeAdmins.head

}
