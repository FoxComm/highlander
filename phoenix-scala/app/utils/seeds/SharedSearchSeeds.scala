package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.account._
import models.admin._
import models.sharedsearch.{SharedSearch, SharedSearchAssociation, SharedSearchAssociations, SharedSearches}
import org.json4s.jackson.JsonMethods._
import utils.db._
import slick.driver.PostgresDriver.api._
import utils.aliases.AU

trait SharedSearchSeeds {

  def createSharedSearches(adminId: Int)(implicit au: AU): DbResultT[SharedSearch] =
    for {
      search        ← * <~ SharedSearches.create(sharedSearch(adminId))
      productSearch ← * <~ SharedSearches.create(archivedProductsSearch(adminId))
      skusSearch    ← * <~ SharedSearches.create(archivedSkusSearch(adminId))
      storeAdmins   ← * <~ AdminsData.sortBy(_.accountId).result
      _ ← * <~ storeAdmins.map { admin ⇒
           SharedSearchAssociations.create(
               new SharedSearchAssociation(sharedSearchId = productSearch.id,
                                           storeAdminId = admin.accountId))
         }
      _ ← * <~ storeAdmins.map { admin ⇒
           SharedSearchAssociations.create(
               new SharedSearchAssociation(sharedSearchId = skusSearch.id,
                                           storeAdminId = admin.accountId))
         }
    } yield search

  def sharedSearch(adminId: Int)(implicit au: AU) =
    SharedSearch(title = "All Products",
                 query = parse("[]"),
                 rawQuery = parse("{}"),
                 storeAdminId = adminId,
                 scope = SharedSearch.ProductsScope,
                 accessScope = Scope.current)

  def archivedProductsSearch(adminId: Int)(implicit au: AU) =
    SharedSearch(title = "Archived",
                 query = parse("""[
                                 |    {
                                 |      "display": "Product : Is Archived : Yes",
                                 |      "term": "archivedAt",
                                 |      "operator": "exists",
                                 |      "value": {
                                 |        "type": "exists"
                                 |      }
                                 |    }
                                 |  ]""".stripMargin),
                 rawQuery = parse("""{
                                  |   "query": {
                                  |      "bool": {
                                  |        "filter": [
                                  |          {
                                  |            "exists": {
                                  |              "field": "archivedAt"
                                  |            }
                                  |          }
                                  |        ]
                                  |      }
                                  |    }
                                  |   }""".stripMargin),
                 storeAdminId = adminId,
                 scope = SharedSearch.ProductsScope,
                 accessScope = Scope.current,
                 isSystem = true)

  def archivedSkusSearch(adminId: Int)(implicit au: AU) =
    SharedSearch(title = "Archived",
                 query = parse("""[
                      |    {
                      |      "display": "Product : Is Archived : Yes",
                      |      "term": "archivedAt",
                      |      "operator": "exists",
                      |      "value": {
                      |        "type": "exists"
                      |      }
                      |    }
                      |  ]""".stripMargin),
                 rawQuery = parse("""{
                         | "query": {
                         |      "bool": {
                         |        "filter": [
                         |          {
                         |            "exists": {
                         |              "field": "archivedAt"
                         |            }
                         |          }
                         |        ]
                         |      }
                         |    }
                         | }""".stripMargin),
                 storeAdminId = adminId,
                 scope = SharedSearch.SkusScope,
                 accessScope = Scope.current,
                 isSystem = true)
}
