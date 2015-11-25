package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.{StoreAdmin, StoreAdmins}
import utils.DbResultT.implicits._
import utils.DbResultT.{DbResultT, _}

trait StoreAdminSeeds {

  // Returns only first admin id
  def createStoreAdmins: DbResultT[StoreAdmin#Id] = for {
    adminIds ← * <~ StoreAdmins.createAllReturningIds(storeAdmins)
  } yield adminIds.head

  def storeAdmins = Seq(
    StoreAdmin(email = "admin@admin.com", password = "password", firstName = "Frankly", lastName = "Admin"),
    StoreAdmin(email = "hackerman@yahoo.com", password = "password1", firstName = "Such", lastName = "Root"),
    StoreAdmin(email = "admin_hero@xakep.ru", password = "password2", firstName = "Admin", lastName = "Hero")
  )

  def storeAdmin = storeAdmins.head

}
