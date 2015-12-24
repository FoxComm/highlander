import scala.concurrent.ExecutionContext.Implicits.global

import akka.http.scaladsl.model.StatusCodes
import Extensions._
import models.{StoreAdmins, SharedSearch, SharedSearches}
import models.SharedSearch.CustomersScope
import payloads.SharedSearchPayload
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._

import org.json4s.jackson.JsonMethods._

class SharedSearchIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

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
}
