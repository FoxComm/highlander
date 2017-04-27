import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.NotFoundFailure404
import failures.SharedSearchFailures._
import models.account._
import models.admin._
import models.sharedsearch.SharedSearch._
import models.sharedsearch.SharedSearchAssociation.{build ⇒ buildAssociation}
import models.sharedsearch._
import org.json4s.jackson.JsonMethods._
import payloads.SharedSearchPayloads._
import payloads.StoreAdminPayloads.CreateStoreAdminPayload
import responses.UserResponse.{Root ⇒ UserRoot, build ⇒ buildUser}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Factories

class SharedSearchIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures {

  val dummyJVal = parse("{}")

  "GET v1/shared-search" - {
    "return an error when not scoped" in new SharedSearchFixture {
      GET(s"v1/shared-search", defaultAdminAuth.jwtCookie.some)
        .mustFailWith400(SharedSearchScopeNotFound)
    }

    "returns an error when given an invalid scope" in new SharedSearchFixture {
      sharedSearchApi
        .scope("arstScope")
        .mustFailWith404(NotFoundFailure404(SharedSearch, "arstScope"))
    }

    "returns only customers searches with the orders scope" in new SharedSearchFixture {
      getByScope("customersScope") must === (Seq(customersSearch))
    }

    "returns only orders searches with the orders scope" in new SharedSearchFixture {
      getByScope("ordersScope") must === (Seq(ordersSearch))
    }

    "returns only storeAdmins searches with the storeAdmins scope" in new SharedSearchFixture {
      getByScope("storeAdminsScope") must === (Seq(storeAdminsSearch))
    }

    "returns only giftCards searches with the giftCards scope" in new SharedSearchFixture {
      getByScope("giftCardsScope") must === (Seq(giftCardsSearch))
    }

    "returns only products searches with the products scope" in new SharedSearchFixture {
      getByScope("productsScope") must === (Seq(productsSearch))
    }

    "returns only inventory searches with the inventory scope" in new SharedSearchFixture {
      getByScope("inventoryScope") must === (Seq(inventorySearch))
    }

    "returns only promotion searches with the promotions scope" in new SharedSearchFixture {
      getByScope("promotionsScope") must === (Seq(promotionsSearch))
    }

    "returns only coupon searches with the coupons scope" in new SharedSearchFixture {
      getByScope("couponsScope") must === (Seq(couponsSearch))
    }

    "returns only searches with the returns scope" in new SharedSearchFixture {
      getByScope("returnsScope") must === (Seq(returnsSearch))
    }

    "returns associated scopes created by different admins" in new SharedSearchAssociationFixture {
      SharedSearchAssociations
        .create(
            SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = defaultAdmin.id))
        .gimme

      getByScope("customersScope") must === (Seq(search))
    }
  }

  "GET v1/shared-search/:code" - {
    "returns shared search by code" in new Fixture {
      val payload = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code    = sharedSearchApi.create(payload).as[SharedSearch].code

      val root = sharedSearchApi(code).get().as[SharedSearch]
      root.title must === ("test")
      root.scope must === (CustomersScope)
    }

    "404 if not found" in {
      sharedSearchApi("nope").get().mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
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

      val payload = SharedSearchPayload("test", parse(query), dummyJVal, CustomersScope)
      val root    = sharedSearchApi.create(payload).as[SharedSearch]

      root.title must === ("test")
      root.scope must === (CustomersScope)
      root.code.isEmpty must === (false)

      SharedSearches.byAdmin(defaultAdmin.id).gimme.size must === (1)
    }

    "400 if query has invalid JSON payload" in new Fixture {
      val query = """
          | {
          |   "title": "Test",
          |   "query": xxx,
          |   "scope": "customersScope"
          | }
        """.stripMargin

      private val message = "The request content was malformed:\n" + SharedSearchInvalidQueryFailure.description
      sharedSearchApi.createFromQuery(query).mustFailWithMessage(message)
    }
  }

  "PATCH v1/shared-search/:code" - {
    "updates shared search" in new Fixture {
      val payload = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code    = sharedSearchApi.create(payload).as[SharedSearch].code

      val updPayload = SharedSearchPayload("new_title", dummyJVal, dummyJVal, CustomersScope)
      val root       = sharedSearchApi(code).update(updPayload).as[SharedSearch]

      root.title must === ("new_title")
      root.scope must === (CustomersScope)
    }

    "404 if not found" in {
      sharedSearchApi("nope")
        .update(SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope))
        .mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
    }

    "400 if query has invalid JSON payload" in new Fixture {
      val payload = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code    = sharedSearchApi.create(payload).as[SharedSearch].code

      val query = """
          | {
          |   "title": "Test",
          |   "query": xxx,
          |   "scope": "customersScope"
          | }
        """.stripMargin

      private val message = "The request content was malformed:\n" + SharedSearchInvalidQueryFailure.description
      sharedSearchApi(code).updateFromQuery(query).mustFailWithMessage(message)
    }
  }

  "DELETE v1/shared-search/:code" - {
    "softly deletes shared search, shouldn't be shown in next request" in new Fixture {
      val payload = SharedSearchPayload("test", dummyJVal, dummyJVal, CustomersScope)
      val code    = sharedSearchApi.create(payload).as[SharedSearch].code
      sharedSearchApi(code).delete().mustBeEmpty()

      sharedSearchApi(code).get().mustFailWith404(NotFoundFailure404(SharedSearch, code))

      getByScope("customersScope") mustBe empty

      SharedSearches.findOneByCode(code).gimme.isDefined must === (true)
    }

    "404 if not found" in {
      sharedSearchApi("nope").delete().mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
    }
  }

  "POST /v1/shared-search/:code/associate" - {

    "can be associated with shared search" in new AssociateBaseFixture {
      sharedSearchApi(search.code)
        .associate(SharedSearchAssociationPayload(Seq(defaultAdmin.id)))
        .asThe[SharedSearch]
        .warnings mustBe empty

      SharedSearchAssociations.bySharedSearch(search).gimme.size mustBe 1
    }

    "404 if shared search is not found" in new AssociateBaseFixture {
      sharedSearchApi("nope")
        .associate(SharedSearchAssociationPayload(Seq(defaultAdmin.id)))
        .mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
    }

    "warning if store admin is not found" in new AssociateBaseFixture {
      sharedSearchApi(search.code)
        .associate(SharedSearchAssociationPayload(Seq(1, 999)))
        .asThe[SharedSearch]
        .errors
        .value must === (Seq(NotFoundFailure404(User, 999).description))
    }

    "do not create duplicate records" in new AssociateBaseFixture {
      val payload = SharedSearchAssociationPayload(Seq(defaultAdmin.id))
      sharedSearchApi(search.code).associate(payload).mustBeOk()
      sharedSearchApi(search.code).associate(payload).mustBeOk()

      SharedSearchAssociations.bySharedSearch(search).gimme.size mustBe 1
    }
  }

  "GET v1/shared-search/:code/associates" - {
    "returns associates by code" in new AssociateSecondaryFixture {
      sharedSearchApi(search.code).associates().as[Seq[UserRoot]].onlyElement.id must === (
          defaultAdmin.id)
    }

    "returns multiple associates by code" in new AssociateSecondaryFixture {
      val secondAdminId = withRandomAdminAuth { implicit auth ⇒
        // move shared search injected creation to an API call here
        auth.adminId
      }

      SharedSearchAssociations
        .create(SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = secondAdminId))
        .gimme

      sharedSearchApi(search.code)
        .associates()
        .as[Seq[UserRoot]]
        .map(_.id) must contain allOf (defaultAdmin.id, secondAdminId)
    }

    "404 if not found" in {
      sharedSearchApi("nope")
        .associates()
        .mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
    }
  }

  "DELETE /v1/shared-search/:code/associate/:storeAdminId" - {

    "can be removed from associates" in new AssociateSecondaryFixture {
      sharedSearchApi(search.code).unassociate(defaultAdmin.id).mustBeOk()

      SharedSearchAssociations.bySharedSearch(search).gimme mustBe empty
    }

    "400 if association is not found" in new AssociateBaseFixture {
      sharedSearchApi(search.code)
        .unassociate(defaultAdmin.id)
        .mustFailWith400(SharedSearchAssociationNotFound(search.code, defaultAdmin.id))
    }

    "404 if sharedSearch is not found" in new AssociateSecondaryFixture {
      sharedSearchApi("nope")
        .unassociate(defaultAdmin.id)
        .mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
    }

    "404 if storeAdmin is not found" in new AssociateSecondaryFixture {
      sharedSearchApi(search.code).unassociate(555).mustFailWith404(NotFoundFailure404(User, 555))
    }
  }

  trait Fixture {
    val scope = Scopes.forOrganization(TENANT).gimme
  }

  trait SharedSearchFixture extends Fixture {
    private def getSharedSearch(scpe: SharedSearch.Scope) =
      SharedSearch(title = s"Some ${scpe.toString}",
                   query = dummyJVal,
                   rawQuery = dummyJVal,
                   scope = scpe,
                   storeAdminId = defaultAdmin.id,
                   accessScope = scope)

    val customerScope   = getSharedSearch(CustomersScope)
    val orderScope      = getSharedSearch(OrdersScope)
    val storeAdminScope = getSharedSearch(StoreAdminsScope)
    val giftCardScope   = getSharedSearch(GiftCardsScope)
    val productScope    = getSharedSearch(ProductsScope)
    val inventoryScope  = getSharedSearch(InventoryScope)
    val promotionsScope = getSharedSearch(PromotionsScope)
    val couponsScope    = getSharedSearch(CouponsScope)
    val returnsScope    = getSharedSearch(ReturnsScope)

    val (customersSearch,
         ordersSearch,
         storeAdminsSearch,
         giftCardsSearch,
         productsSearch,
         inventorySearch,
         promotionsSearch,
         couponsSearch,
         returnsSearch) = (for {
      storeAdmin        ← * <~ Users.mustFindByAccountId(defaultAdmin.id)
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
      returnsSearch     ← * <~ SharedSearches.create(returnsScope)
      _                 ← * <~ SharedSearchAssociations.create(buildAssociation(returnsSearch, storeAdmin))
    } yield
      (customersSearch,
       ordersSearch,
       storeAdminsSearch,
       giftCardsSearch,
       productsSearch,
       inventorySearch,
       promotionsSearch,
       couponsSearch,
       returnsSearch)).gimme
  }

  trait SharedSearchAssociationFixture extends Fixture {
    val secondAdminId = withRandomAdminAuth { implicit auth ⇒
      // move shared search injected creation to an API call here
      auth.adminId
    }
    val search = (for {
      search ← * <~ SharedSearches.create(
                  SharedSearch(title = "Test",
                               query = dummyJVal,
                               rawQuery = dummyJVal,
                               scope = CustomersScope,
                               storeAdminId = secondAdminId,
                               accessScope = scope))
      secondAdmin ← * <~ Users.mustFindByAccountId(secondAdminId)
      _           ← * <~ SharedSearchAssociations.create(buildAssociation(search, secondAdmin))
    } yield search).gimme
  }

  trait AssociateBaseFixture extends Fixture {
    val customerScope = SharedSearch(title = "Active Customers",
                                     query = dummyJVal,
                                     rawQuery = dummyJVal,
                                     scope = CustomersScope,
                                     storeAdminId = defaultAdmin.id,
                                     accessScope = scope)

    val search = SharedSearches.create(customerScope).gimme
  }

  trait AssociateSecondaryFixture extends AssociateBaseFixture {
    val associate = SharedSearchAssociations
      .create(SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = defaultAdmin.id))
      .gimme
  }

  private def getByScope(scope: String): Seq[SharedSearch] =
    sharedSearchApi.scope(scope).as[Seq[SharedSearch]]

}
