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
import responses.UserResponse.{Root ⇒ UserRoot, build ⇒ buildUser}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
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
      GET(s"v1/shared-search").mustFailWith400(SharedSearchScopeNotFound)
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

    "returns associated scopes created by different admins" in new SharedSearchAssociationFixture {
      SharedSearchAssociations.create(buildAssociation(search, storeAdmin)).gimme

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

      SharedSearches.byAdmin(storeAdmin.accountId).gimme.size must === (1)
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
        .associate(SharedSearchAssociationPayload(Seq(storeAdmin.accountId)))
        .asThe[SharedSearch]
        .warnings mustBe empty

      SharedSearchAssociations.bySharedSearch(search).gimme.size mustBe 1
    }

    "404 if shared search is not found" in new AssociateBaseFixture {
      sharedSearchApi("nope")
        .associate(SharedSearchAssociationPayload(Seq(storeAdmin.accountId)))
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
      val payload = SharedSearchAssociationPayload(Seq(storeAdmin.accountId))
      sharedSearchApi(search.code).associate(payload).mustBeOk()
      sharedSearchApi(search.code).associate(payload).mustBeOk()

      SharedSearchAssociations.bySharedSearch(search).gimme.size mustBe 1
    }
  }

  "GET v1/shared-search/:code/associates" - {
    "returns associates by code" in new AssociateSecondaryFixture {
      val root = sharedSearchApi(search.code).associates().as[Seq[UserRoot]]
      root must === (Seq(buildUser(storeAdmin)))
    }

    "returns multiple associates by code" in new AssociateSecondaryFixture {
      SharedSearchAssociations.create(buildAssociation(search, secondAdmin)).gimme

      val root = sharedSearchApi(search.code).associates().as[Seq[UserRoot]]
      root must contain allOf (buildUser(storeAdmin), buildUser(secondAdmin))
    }

    "404 if not found" in {
      sharedSearchApi("nope")
        .associates()
        .mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
    }
  }

  "DELETE /v1/shared-search/:code/associate/:storeAdminId" - {

    "can be removed from associates" in new AssociateSecondaryFixture {
      sharedSearchApi(search.code).unassociate(storeAdmin.accountId).mustBeOk()

      SharedSearchAssociations.bySharedSearch(search).gimme mustBe empty
    }

    "400 if association is not found" in new AssociateSecondaryFixture {
      sharedSearchApi(search.code)
        .unassociate(secondAdmin.id)
        .mustFailWith400(SharedSearchAssociationNotFound(search.code, secondAdmin.id))
    }

    "404 if sharedSearch is not found" in new AssociateSecondaryFixture {
      sharedSearchApi("nope")
        .unassociate(storeAdmin.accountId)
        .mustFailWith404(NotFoundFailure404(SharedSearch, "nope"))
    }

    "404 if storeAdmin is not found" in new AssociateSecondaryFixture {
      sharedSearchApi(search.code).unassociate(555).mustFailWith404(NotFoundFailure404(User, 555))
    }
  }

  trait Fixture {
    val scope             = Scopes.forOrganization(TENANT).gimme
    val storeAdminAccount = Accounts.create(Account()).gimme
    val storeAdmin        = Users.create(authedUser.copy(accountId = storeAdminAccount.id)).gimme
    val storeAdminUser = AdminsData
      .create(
        AdminData(userId = storeAdmin.id,
                  accountId = storeAdmin.accountId,
                  state = AdminData.Active,
                  scope = scope))
      .gimme
  }

  trait SharedSearchFixture extends Fixture {
    val customerScope = SharedSearch(title = "Active Customers",
                                     query = dummyJVal,
                                     rawQuery = dummyJVal,
                                     scope = CustomersScope,
                                     storeAdminId = storeAdmin.accountId)
    val orderScope = SharedSearch(title = "Manual Hold",
                                  query = dummyJVal,
                                  rawQuery = dummyJVal,
                                  scope = OrdersScope,
                                  storeAdminId = storeAdmin.accountId)
    val storeAdminScope = SharedSearch(title = "Some Store Admin",
                                       query = dummyJVal,
                                       rawQuery = dummyJVal,
                                       scope = StoreAdminsScope,
                                       storeAdminId = storeAdmin.accountId)
    val giftCardScope = SharedSearch(title = "Some Gift Card",
                                     query = dummyJVal,
                                     rawQuery = dummyJVal,
                                     scope = GiftCardsScope,
                                     storeAdminId = storeAdmin.accountId)
    val productScope = SharedSearch(title = "Some Product",
                                    query = dummyJVal,
                                    rawQuery = dummyJVal,
                                    scope = ProductsScope,
                                    storeAdminId = storeAdmin.accountId)
    val inventoryScope = SharedSearch(title = "Some Inventory",
                                      query = dummyJVal,
                                      rawQuery = dummyJVal,
                                      scope = InventoryScope,
                                      storeAdminId = storeAdmin.accountId)
    val promotionsScope = SharedSearch(title = "Some Promotions",
                                       query = dummyJVal,
                                       rawQuery = dummyJVal,
                                       scope = PromotionsScope,
                                       storeAdminId = storeAdmin.accountId)
    val couponsScope = SharedSearch(title = "Some Coupons",
                                    query = dummyJVal,
                                    rawQuery = dummyJVal,
                                    scope = CouponsScope,
                                    storeAdminId = storeAdmin.accountId)

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
    def scope: LTree

    val secondAccount = Accounts.create(Account()).gimme
    val secondAdmin = Users
      .create(
        Factories.storeAdmin.copy(accountId = secondAccount.id,
                                  name = "Junior".some,
                                  email = "another@domain.com".some))
      .gimme
    val secondAdminUser = AdminsData
      .create(
        AdminData(userId = secondAdmin.id,
                  accountId = secondAdmin.accountId,
                  state = AdminData.Active,
                  scope = scope))
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
                                     storeAdminId = storeAdmin.accountId)

    val search = SharedSearches.create(customerScope).gimme
  }

  trait AssociateSecondaryFixture extends AssociateBaseFixture with SecondAdminFixture {
    val associate = SharedSearchAssociations.create(buildAssociation(search, storeAdmin)).gimme
  }

  private def getByScope(scope: String): Seq[SharedSearch] =
    sharedSearchApi.scope(scope).as[Seq[SharedSearch]]

}
