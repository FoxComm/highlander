import scala.concurrent.ExecutionContext.Implicits.global

import akka.http.scaladsl.model.StatusCodes
import Extensions._
import models.{SharedSearchAssociations, StoreAdmin, StoreAdmins, SharedSearch, SharedSearches}
import models.SharedSearchAssociation.{build ⇒ buildAssociation}
import models.SharedSearch.{CustomersScope, OrdersScope, StoreAdminsScope}
import payloads.{SharedSearchPayload, SharedSearchAssociationPayload}
import services.{SharedSearchAssociationNotFound, NotFoundFailure404}
import responses.StoreAdminResponse.{Root ⇒ AdminRoot, build ⇒ buildAdmin}
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds.Factories
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

    "returns associated scopes created by different admins" in new SharedSearchAssociationFixture {
      SharedSearchAssociations.create(buildAssociation(search, storeAdmin)).run().futureValue

      val response = GET(s"v1/shared-search")
      response.status must === (StatusCodes.OK)

      val expectedResponse = Seq(search)
      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (expectedResponse)
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
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
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
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "DELETE v1/shared-search/:code" - {
    "softly deletes shared search, shouldn't be shown in next request" in new Fixture {
      val code = POST(s"v1/shared-search", SharedSearchPayload("test", parse("{}"), CustomersScope)).as[SharedSearch].code
      val response = DELETE(s"v1/shared-search/$code")
      response.status must === (StatusCodes.NoContent)

      val getResponse = GET(s"v1/shared-search/$code")
      getResponse.status must === (StatusCodes.NotFound)

      SharedSearches.findOneByCode(code).run().futureValue.isDefined must === (true)
    }

    "404 if not found" in {
      val response = DELETE(s"v1/shared-search/nope")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "POST /v1/shared-search/:code/associate" - {

    "can be associated with shared search" in new AssociateBaseFixture {
      val response = POST(s"v1/shared-search/${search.code}/associate", SharedSearchAssociationPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      val searchWithWarnings = response.withResultTypeOf[SharedSearch]
      searchWithWarnings.warnings mustBe empty

      SharedSearchAssociations.bySharedSearch(search).result.run().futureValue.size mustBe 1
    }

    "404 if shared search is not found" in new AssociateBaseFixture {
      val response = POST(s"v1/shared-search/nope/associate", SharedSearchAssociationPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }

    "warning if store admin is not found" in new AssociateBaseFixture {
      val response = POST(s"v1/shared-search/${search.code}/associate", SharedSearchAssociationPayload(Seq(1, 999)))
      response.status must === (StatusCodes.OK)

      val searchWithWarnings = response.withResultTypeOf[SharedSearch]
      searchWithWarnings.warnings.value must === (List(NotFoundFailure404(StoreAdmin, 999).description))
    }

    "do not create duplicate records" in new AssociateBaseFixture {
      POST(s"v1/shared-search/${search.code}/associate", SharedSearchAssociationPayload(Seq(storeAdmin.id)))
      POST(s"v1/shared-search/${search.code}/associate", SharedSearchAssociationPayload(Seq(storeAdmin.id)))

      SharedSearchAssociations.bySharedSearch(search).result.run().futureValue.size mustBe 1
    }
  }

  "GET v1/shared-search/:code/associates" - {
    "returns associates by code" in new AssociateSecondaryFixture {
      val response = GET(s"v1/shared-search/${search.code}/associates")
      response.status must === (StatusCodes.OK)

      val root = response.as[Seq[AdminRoot]]
      root must === (Seq(storeAdmin).map(buildAdmin))
    }

    "returns multiple associates by code" in new AssociateSecondaryFixture {
      SharedSearchAssociations.create(buildAssociation(search, secondAdmin)).run().futureValue

      val response = GET(s"v1/shared-search/${search.code}/associates")
      response.status must === (StatusCodes.OK)

      val root = response.as[Seq[AdminRoot]]
      root must === (Seq(storeAdmin, secondAdmin).map(buildAdmin))
    }

    "404 if not found" in {
      val response = GET(s"v1/shared-search/nope/associates")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "DELETE /v1/shared-search/:code/associate/:storeAdminId" - {

    "can be removed from associates" in new AssociateSecondaryFixture {
      val response = DELETE(s"v1/shared-search/${search.code}/associate/${storeAdmin.id}")
      response.status must === (StatusCodes.OK)

      SharedSearchAssociations.bySharedSearch(search).result.run().futureValue mustBe empty
    }

    "400 if association is not found" in new AssociateSecondaryFixture {
      val response = DELETE(s"v1/shared-search/${search.code}/associate/${secondAdmin.id}")
      response.status must === (StatusCodes.BadRequest)
      response.error must === (SharedSearchAssociationNotFound(search.code, secondAdmin.id).description)
    }

    "404 if sharedSearch is not found" in new AssociateSecondaryFixture {
      val response = DELETE(s"v1/shared-search/nope/associate/${storeAdmin.id}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }

    "404 if storeAdmin is not found" in new AssociateSecondaryFixture {
      val response = DELETE(s"v1/shared-search/${search.code}/associate/555")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 555).description)
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
      customersSearch   ← * <~ SharedSearches.create(customerScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(customersSearch, storeAdmin))
      ordersSearch      ← * <~ SharedSearches.create(orderScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(ordersSearch, storeAdmin))
      storeAdminsSearch ← * <~ SharedSearches.create(storeAdminScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(storeAdminsSearch, storeAdmin))
    } yield (customersSearch, ordersSearch, storeAdminsSearch)).runTxn().futureValue.rightVal
  }

  trait SharedSearchAssociationFixture extends Fixture {
    val (secondAdmin, search) = (for {
      secondAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      search      ← * <~ SharedSearches.create(SharedSearch(title = "Test", query = parse("{}"),
        scope = CustomersScope, storeAdminId = secondAdmin.id))
      _           ← * <~ SharedSearchAssociations.create(buildAssociation(search, secondAdmin))
    } yield (secondAdmin, search)).runTxn().futureValue.rightVal
  }

  trait AssociateBaseFixture extends Fixture {
    val customerScope = SharedSearch(title = "Active Customers", query = parse("{}"),
      scope = CustomersScope, storeAdminId = storeAdmin.id)

    val search = SharedSearches.create(customerScope).run().futureValue.rightVal
  }

  trait AssociateSecondaryFixture extends AssociateBaseFixture {
    val (associate, secondAdmin) = (for {
      associate   ← * <~ SharedSearchAssociations.create(buildAssociation(search, storeAdmin))
      secondAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (associate, secondAdmin)).runTxn().futureValue.rightVal
  }
}
