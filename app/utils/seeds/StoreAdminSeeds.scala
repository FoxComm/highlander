package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.{StoreAdmin, StoreAdmins}
import utils.DbResultT.implicits._
import utils.DbResultT.{DbResultT, _}
import cats.implicits._

trait StoreAdminSeeds {

  // Returns only first admin id
  def createStoreAdmins: DbResultT[StoreAdmin#Id] = for {
    adminIds ← * <~ StoreAdmins.createAllReturningIds(storeAdmins)
  } yield adminIds.head

  def storeAdmins = Seq(
    StoreAdmin.build(email = "admin@admin.com", password = "password".some, name = "Frankly Admin"),
    StoreAdmin.build(email = "hackerman@yahoo.com", password = "password1".some, name = "Such Root"),
    StoreAdmin.build(email = "admin_hero@xakep.ru", password = "password2".some, name = "Admin Hero")
  )

  def storeAdmin = storeAdmins.head

}
