import scala.concurrent.ExecutionContext.Implicits.global

import akka.http.scaladsl.model.StatusCodes
import Extensions._
import models.{StoreAdmins, SharedSearch, SharedSearches}
import models.SharedSearch.{CustomersScope, OrdersScope, StoreAdminsScope}
import payloads.SharedSearchPayload
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._

import org.json4s.jackson.JsonMethods._

class SharedSearchIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GET v1/shared-search" - {
    "returns all searches when not scoped" in new SharedSearchFixture {
      val response = GET(s"v1/shared-search")
      response.status must === (StatusCodes.OK)

      val expectedResponse = Seq(customersSearch, ordersSearch, storeAdminsSearch)
      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (expectedResponse)
    }

    "returns only customers searches with the orders scope" in new SharedSearchFixture {
      val response = GET(s"v1/shared-search?scope=customersScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(customersSearch))
    }

    "returns only orders searches with the orders scope" in new SharedSearchFixture {
      val response = GET(s"v1/shared-search?scope=ordersScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(ordersSearch))
    }

    "returns only storeAdmins searches with the storeAdmins scope" in new SharedSearchFixture {
      val response = GET(s"v1/shared-search?scope=storeAdminsScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(storeAdminsSearch))
    }
  }

  "GET v1/shared-search/:code" - {
    "returns shared search by code" in new Fixture {
      val code = POST(s"v1/shared-search", SharedSearchPayload("test", parse("{}"), CustomersScope)).as[SharedSearch].code
      val response = GET(s"v1/shared-search/$code")
      response.status must === (StatusCodes.OK)

      val root = response.as[SharedSearch]
      root.title must === ("test")
      root.scope must === (CustomersScope)
    }

    "404 if not found" in {
      val response = GET(s"v1/shared-search/nope")
      response.status must === (StatusCodes.NotFound)
      response.errors must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "POST v1/shared-search" - {
    "creates a shared search" in new Fixture {
      val query =
        """
          | {
          |   "filter_bool": true,
          |   "filter_array": [1, 2, 3],
          |   "filter_obj": {"field": "value"}
          | }
        """.stripMargin
      val response = POST(s"v1/shared-search", SharedSearchPayload("test", parse(query), CustomersScope))
      response.status must === (StatusCodes.OK)

      val root = response.as[SharedSearch]
      root.title must === ("test")
      root.scope must === (CustomersScope)
      root.code.isEmpty must === (false)

      SharedSearches.byAdmin(storeAdmin.id).result.run().futureValue.size must === (1)
    }
  }

  "PATCH v1/shared-search/:code" - {
    "updates shared search" in new Fixture {
      val code = POST(s"v1/shared-search", SharedSearchPayload("test", parse("{}"), CustomersScope)).as[SharedSearch].code
      val response = PATCH(s"v1/shared-search/$code", SharedSearchPayload("new_title", parse("{}"), CustomersScope))
      response.status must === (StatusCodes.OK)

      val root = response.as[SharedSearch]
      root.title must === ("new_title")
      root.scope must === (CustomersScope)
    }

    "404 if not found" in {
      val response = PATCH(s"v1/shared-search/nope", SharedSearchPayload("test", parse("{}"), CustomersScope))
      response.status must === (StatusCodes.NotFound)
      response.errors must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "DELETE v1/shared-search/:code" - {
    "deletes shared search" in new Fixture {
      val code = POST(s"v1/shared-search", SharedSearchPayload("test", parse("{}"), CustomersScope)).as[SharedSearch].code
      val response = DELETE(s"v1/shared-search/$code")
      response.status must === (StatusCodes.NoContent)
    }

    "404 if not found" in {
      val response = DELETE(s"v1/shared-search/nope")
      response.status must === (StatusCodes.NotFound)
      response.errors must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  trait Fixture {
    val storeAdmin = StoreAdmins.create(authedStoreAdmin).run().futureValue.rightVal
  }

  trait SharedSearchFixture extends Fixture {
    val customerScope = SharedSearch(title = "Active Customers", query = parse("{}"),
      scope = CustomersScope, storeAdminId = storeAdmin.id)
    val orderScope = SharedSearch(title = "Manual Hold", query = parse("{}"),
      scope = OrdersScope, storeAdminId = storeAdmin.id)
    val storeAdminScope = SharedSearch(title = "Some Store Admin", query = parse("{}"),
      scope = StoreAdminsScope, storeAdminId = storeAdmin.id)

    val (customersSearch, ordersSearch, storeAdminsSearch) = (for {
      customersSearch ← * <~ SharedSearches.create(customerScope)
      ordersSearch ← * <~ SharedSearches.create(orderScope)
      storeAdminsSearch ← * <~ SharedSearches.create(storeAdminScope)
    } yield (customersSearch, ordersSearch, storeAdminsSearch)).runTxn().futureValue.rightVal
  }
}
