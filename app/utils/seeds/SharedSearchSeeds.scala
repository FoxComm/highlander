package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.sharedsearch.{SharedSearch, SharedSearches}
import utils.db._
import utils.db.DbResultT._

import org.json4s.jackson.JsonMethods._

trait SharedSearchSeeds {

  def createSharedSearches(adminId: Int): DbResultT[SharedSearch] =
    for {
      search ← * <~ SharedSearches.create(sharedSearch(adminId))
    } yield search

  def sharedSearch(adminId: Int) =
    SharedSearch(title = "All Products",
                 query = parse("[]"),
                 rawQuery = parse("{}"),
                 storeAdminId = adminId,
                 scope = SharedSearch.ProductsScope)
}
