import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.NotFoundFailure404
import failures.SharedSearchFailures._
import models.sharedsearch.SharedSearch._
import models.sharedsearch.SharedSearchAssociation.{build ⇒ buildAssociation}
import models.sharedsearch._
import models.{StoreAdmin, StoreAdmins}
import org.json4s.jackson.JsonMethods._
import payloads.SharedSearchPayloads._
import responses.StoreAdminResponse.{Root ⇒ AdminRoot, build ⇒ buildAdmin}
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class SharedSearchIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  val dummyJVal = parse("{}")

  "GET v1/shared-search" - {
    "return an error when not scoped" in new SharedSearchFixture {
      val response = GET(s"v1/shared-search")
      response.status must === (StatusCodes.BadRequest)
      response.error must === (SharedSearchScopeNotFound.description)
    }

    "returns an error when given an invalid scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("arstScope")
      response.status must === (StatusCodes.NotFound)
    }

    "returns only customers searches with the orders scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("customersScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(customersSearch))
    }

    "returns only orders searches with the orders scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("ordersScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(ordersSearch))
    }

    "returns only storeAdmins searches with the storeAdmins scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("storeAdminsScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(storeAdminsSearch))
    }

    "returns only giftCards searches with the giftCards scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("giftCardsScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(giftCardsSearch))
    }

    "returns only products searches with the products scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("productsScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(productsSearch))
    }

    "returns only inventory searches with the inventory scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("inventoryScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(inventorySearch))
    }

    "returns only promotion searches with the promotions scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("promotionsScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(promotionsSearch))
    }

    "returns only coupon searches with the coupons scope" in new SharedSearchFixture {
      val response = sharedSearchApi.scope("couponsScope")
      response.status must === (StatusCodes.OK)

      val searchResponse = response.as[Seq[SharedSearch]]
      searchResponse must === (Seq(couponsSearch))
    }

    "returns associated scopes created by different admins" in new SharedSearchAssociationFixture {
      SharedSearchAssociations.create(buildAssociation(search, storeAdmin)).gimme

      val response = sharedSearchApi.scope("customersScope")
      response.status must === (StatusCodes.OK)

      val expectedResponse = Seq(search)
      val searchResponse   = response.as[Seq[SharedSearch]]
      searchResponse must === (expectedResponse)
    }
  }

  "GET v1/shared-search/:code" - {
    "returns shared search by code" in new Fixture {
      val payload  = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code     = sharedSearchApi.create(payload).as[SharedSearch].code
      val response = sharedSearchApi(code).get()
      response.status must === (StatusCodes.OK)

      val root = response.as[SharedSearch]
      root.title must === ("test")
      root.scope must === (CustomersScope)
    }

    "404 if not found" in {
      val response = sharedSearchApi("nope").get()
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "POST v1/shared-search" - {
    "creates a shared search" in new Fixture {
      val query = """
          | {
          |   "filter_bool": true,
          |   "filter_array": [1, 2, 3],
          |   "filter_obj": {"field": "value"}
          | }
        """.stripMargin

      val payload  = SharedSearchPayload("test", parse(query), dummyJVal, CustomersScope)
      val response = sharedSearchApi.create(payload)
      response.status must === (StatusCodes.OK)

      val root = response.as[SharedSearch]
      root.title must === ("test")
      root.scope must === (CustomersScope)
      root.code.isEmpty must === (false)

      SharedSearches.byAdmin(storeAdmin.id).gimme.size must === (1)
    }

    "400 if query has invalid JSON payload" in new Fixture {
      val query    = """
          | {
          |   "title": "Test",
          |   "query": xxx,
          |   "scope": "customersScope"
          | }
        """.stripMargin
      val response = sharedSearchApi.createFromQuery(query)
      response.status must === (StatusCodes.BadRequest)
      response.error must include(SharedSearchInvalidQueryFailure.description)
    }
  }

  "PATCH v1/shared-search/:code" - {
    "updates shared search" in new Fixture {
      val payload = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code    = sharedSearchApi.create(payload).as[SharedSearch].code

      val updPayload = SharedSearchPayload("new_title", dummyJVal, dummyJVal, CustomersScope)
      val response   = sharedSearchApi(code).update(updPayload)
      response.status must === (StatusCodes.OK)

      val root = response.as[SharedSearch]
      root.title must === ("new_title")
      root.scope must === (CustomersScope)
    }

    "404 if not found" in {
      val payload  = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val response = sharedSearchApi("nope").update(payload)
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }

    "400 if query has invalid JSON payload" in new Fixture {
      val payload = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code    = sharedSearchApi.create(payload).as[SharedSearch].code

      val query    = """
          | {
          |   "title": "Test",
          |   "query": xxx,
          |   "scope": "customersScope"
          | }
        """.stripMargin
      val response = sharedSearchApi(code).updateFromQuery(query)
      response.status must === (StatusCodes.BadRequest)
      response.error must include(SharedSearchInvalidQueryFailure.description)
    }
  }

  "DELETE v1/shared-search/:code" - {
    "softly deletes shared search, shouldn't be shown in next request" in new Fixture {
      val payload  = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code     = sharedSearchApi.create(payload).as[SharedSearch].code
      val response = sharedSearchApi(code).delete()
      response.status must === (StatusCodes.NoContent)

      val getResponse = sharedSearchApi(code).get()
      getResponse.status must === (StatusCodes.NotFound)

      val searchResponse = sharedSearchApi.scope("customersScope")
      searchResponse.status must === (StatusCodes.OK)

      searchResponse.as[Seq[SharedSearch]] must === (Seq.empty[SharedSearch])

      SharedSearches.findOneByCode(code).gimme.isDefined must === (true)
    }

    "404 if not found" in {
      val response = sharedSearchApi("nope").delete()
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "POST /v1/shared-search/:code/associate" - {

    "can be associated with shared search" in new AssociateBaseFixture {
      val response =
        sharedSearchApi(search.code).associate(SharedSearchAssociationPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      val searchWithWarnings = response.withResultTypeOf[SharedSearch]
      searchWithWarnings.warnings mustBe empty

      SharedSearchAssociations.bySharedSearch(search).gimme.size mustBe 1
    }

    "404 if shared search is not found" in new AssociateBaseFixture {
      val response =
        sharedSearchApi("nope").associate(SharedSearchAssociationPayload(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }

    "warning if store admin is not found" in new AssociateBaseFixture {
      val response =
        sharedSearchApi(search.code).associate(SharedSearchAssociationPayload(Seq(1, 999)))
      response.status must === (StatusCodes.OK)

      val searchWithWarnings = response.withResultTypeOf[SharedSearch]
      searchWithWarnings.errors.value must === (
          List(NotFoundFailure404(StoreAdmin, 999).description))
    }

    "do not create duplicate records" in new AssociateBaseFixture {
      sharedSearchApi(search.code).associate(SharedSearchAssociationPayload(Seq(storeAdmin.id)))
      sharedSearchApi(search.code).associate(SharedSearchAssociationPayload(Seq(storeAdmin.id)))

      SharedSearchAssociations.bySharedSearch(search).gimme.size mustBe 1
    }
  }

  "GET v1/shared-search/:code/associates" - {
    "returns associates by code" in new AssociateSecondaryFixture {
      val response = sharedSearchApi(search.code).associates()
      response.status must === (StatusCodes.OK)

      val root = response.as[Seq[AdminRoot]]
      root must === (Seq(storeAdmin).map(buildAdmin))
    }

    "returns multiple associates by code" in new AssociateSecondaryFixture {
      SharedSearchAssociations.create(buildAssociation(search, secondAdmin)).gimme

      val response = sharedSearchApi(search.code).associates()
      response.status must === (StatusCodes.OK)

      val root = response.as[Seq[AdminRoot]]
      root must contain allOf (buildAdmin(storeAdmin), buildAdmin(secondAdmin))
    }

    "404 if not found" in {
      val response = sharedSearchApi("nope").associates()
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }
  }

  "DELETE /v1/shared-search/:code/associate/:storeAdminId" - {

    "can be removed from associates" in new AssociateSecondaryFixture {
      val response = sharedSearchApi(search.code).unassociate(storeAdmin.id)
      response.status must === (StatusCodes.OK)

      SharedSearchAssociations.bySharedSearch(search).gimme mustBe empty
    }

    "400 if association is not found" in new AssociateSecondaryFixture {
      val response = sharedSearchApi(search.code).unassociate(secondAdmin.id)
      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          SharedSearchAssociationNotFound(search.code, secondAdmin.id).description)
    }

    "404 if sharedSearch is not found" in new AssociateSecondaryFixture {
      val response = sharedSearchApi("nope").unassociate(storeAdmin.id)
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SharedSearch, "nope").description)
    }

    "404 if storeAdmin is not found" in new AssociateSecondaryFixture {
      val response = sharedSearchApi(search.code).unassociate(555)
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 555).description)
    }
  }

  trait Fixture {
    val storeAdmin = StoreAdmins.create(authedStoreAdmin).gimme
  }

  trait SharedSearchFixture extends Fixture {
    val customerScope = SharedSearch(title = "Active Customers",
                                     query = dummyJVal,
                                     rawQuery = dummyJVal,
                                     scope = CustomersScope,
                                     storeAdminId = storeAdmin.id)
    val orderScope = SharedSearch(title = "Manual Hold",
                                  query = dummyJVal,
                                  rawQuery = dummyJVal,
                                  scope = OrdersScope,
                                  storeAdminId = storeAdmin.id)
    val storeAdminScope = SharedSearch(title = "Some Store Admin",
                                       query = dummyJVal,
                                       rawQuery = dummyJVal,
                                       scope = StoreAdminsScope,
                                       storeAdminId = storeAdmin.id)
    val giftCardScope = SharedSearch(title = "Some Gift Card",
                                     query = dummyJVal,
                                     rawQuery = dummyJVal,
                                     scope = GiftCardsScope,
                                     storeAdminId = storeAdmin.id)
    val productScope = SharedSearch(title = "Some Product",
                                    query = dummyJVal,
                                    rawQuery = dummyJVal,
                                    scope = ProductsScope,
                                    storeAdminId = storeAdmin.id)
    val inventoryScope = SharedSearch(title = "Some Inventory",
                                      query = dummyJVal,
                                      rawQuery = dummyJVal,
                                      scope = InventoryScope,
                                      storeAdminId = storeAdmin.id)
    val promotionsScope = SharedSearch(title = "Some Promotions",
                                       query = dummyJVal,
                                       rawQuery = dummyJVal,
                                       scope = PromotionsScope,
                                       storeAdminId = storeAdmin.id)
    val couponsScope = SharedSearch(title = "Some Coupons",
                                    query = dummyJVal,
                                    rawQuery = dummyJVal,
                                    scope = CouponsScope,
                                    storeAdminId = storeAdmin.id)

    val (customersSearch,
         ordersSearch,
         storeAdminsSearch,
         giftCardsSearch,
         productsSearch,
         inventorySearch,
         promotionsSearch,
         couponsSearch) = (for {
      customersSearch   ← * <~ SharedSearches.create(customerScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(customersSearch, storeAdmin))
      ordersSearch      ← * <~ SharedSearches.create(orderScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(ordersSearch, storeAdmin))
      storeAdminsSearch ← * <~ SharedSearches.create(storeAdminScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(storeAdminsSearch, storeAdmin))
      giftCardsSearch   ← * <~ SharedSearches.create(giftCardScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(giftCardsSearch, storeAdmin))
      productsSearch    ← * <~ SharedSearches.create(productScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(productsSearch, storeAdmin))
      inventorySearch   ← * <~ SharedSearches.create(inventoryScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(inventorySearch, storeAdmin))
      promotionsSearch  ← * <~ SharedSearches.create(promotionsScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(promotionsSearch, storeAdmin))
      couponsSearch     ← * <~ SharedSearches.create(couponsScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(couponsSearch, storeAdmin))
    } yield
      (customersSearch,
       ordersSearch,
       storeAdminsSearch,
       giftCardsSearch,
       productsSearch,
       inventorySearch,
       promotionsSearch,
       couponsSearch)).gimme
  }

  trait SecondAdminFixture {
    val secondAdmin = StoreAdmins
      .create(Factories.storeAdmin.copy(name = "Junior", email = "another@domain.com"))
      .gimme
  }

  trait SharedSearchAssociationFixture extends Fixture with SecondAdminFixture {
    val search = (for {
      search ← * <~ SharedSearches.create(
                  SharedSearch(title = "Test",
                               query = dummyJVal,
                               rawQuery = dummyJVal,
                               scope = CustomersScope,
                               storeAdminId = secondAdmin.id))
      _ ← * <~ SharedSearchAssociations.create(buildAssociation(search, secondAdmin))
    } yield search).gimme
  }

  trait AssociateBaseFixture extends Fixture {
    val customerScope = SharedSearch(title = "Active Customers",
                                     query = dummyJVal,
                                     rawQuery = dummyJVal,
                                     scope = CustomersScope,
                                     storeAdminId = storeAdmin.id)

    val search = SharedSearches.create(customerScope).gimme
  }

  trait AssociateSecondaryFixture extends AssociateBaseFixture with SecondAdminFixture {
    val associate = SharedSearchAssociations.create(buildAssociation(search, storeAdmin)).gimme
  }
}
