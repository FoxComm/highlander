package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import models.objects.ObjectForm
import models.payment.PaymentMethod
import models.returns.ReturnLineItem
import payloads.ActivityTrailPayloads._
import payloads.AddressPayloads._
import payloads.AssignmentPayloads._
import payloads.CartPayloads._
import payloads.CategoryPayloads._
import payloads.CouponPayloads._
import payloads.CustomerGroupPayloads._
import payloads.CustomerPayloads._
import payloads.GenericTreePayloads._
import payloads.GiftCardPayloads._
import payloads.ImagePayloads._
import payloads.LineItemPayloads._
import payloads.NotePayloads._
import payloads.OrderPayloads._
import payloads.PaymentPayloads._
import payloads.ProductPayloads._
import payloads.PromotionPayloads.{CreatePromotion, UpdatePromotion}
import payloads.ReturnPayloads._
import payloads.SharedSearchPayloads._
import payloads.SkuPayloads._
import payloads.StoreAdminPayloads._
import payloads.StoreCreditPayloads._
import payloads.TaxonomyPayloads.{CreateTaxonPayload, CreateTaxonomyPayload, UpdateTaxonPayload, UpdateTaxonomyPayload}
import payloads.UserPayloads._
import payloads.VariantPayloads._
import payloads._
import testutils._
import utils.aliases.OC

/*
 * prefix → string literal prefix, e.g. "/customers"
 * path   → "compiled" path, e.g. "/customers/1"
 */
trait PhoenixAdminApi extends HttpSupport { self: FoxSuite ⇒

  private val rootPrefix = "v1"

  object customersApi {
    val customersPrefix = s"$rootPrefix/customers"

    def create(payload: CreateCustomerPayload): HttpResponse =
      POST(customersPrefix, payload)
  }

  case class customersApi(id: Int) {
    val customerPath = s"${customersApi.customersPrefix}/$id"

    def get(): HttpResponse =
      GET(customerPath)

    def update(payload: UpdateCustomerPayload): HttpResponse =
      PATCH(s"$customerPath", payload)

    def activate(payload: ActivateCustomerPayload): HttpResponse =
      POST(s"$customerPath/activate", payload)

    def disable(payload: ToggleUserDisabled): HttpResponse =
      POST(s"$customerPath/disable", payload)

    def blacklist(payload: ToggleUserBlacklisted): HttpResponse =
      POST(s"$customerPath/blacklist", payload)

    def cart(): HttpResponse =
      GET(s"$customerPath/cart")

    object addresses {
      val addressesPrefix = s"$customerPath/addresses"

      def get(): HttpResponse =
        GET(addressesPrefix)

      def create(payload: CreateAddressPayload): HttpResponse =
        POST(addressesPrefix, payload)

      def unsetDefault(): HttpResponse =
        DELETE(s"$addressesPrefix/default")
    }

    case class address(id: Int) {
      val addressPath = s"${addresses.addressesPrefix}/$id"

      def get(): HttpResponse =
        GET(addressPath)

      def edit(payload: CreateAddressPayload): HttpResponse =
        PATCH(addressPath, payload)

      def delete(): HttpResponse =
        DELETE(addressPath)

      def setDefault(): HttpResponse =
        POST(s"$addressPath/default")
    }

    object payments {
      val paymentPrefix = s"$customerPath/payment-methods"

      object creditCards {
        val creditCardsPrefix = s"$paymentPrefix/credit-cards"

        def get(): HttpResponse =
          GET(creditCardsPrefix)

        def create(payload: CreateCreditCardFromTokenPayload): HttpResponse =
          POST(creditCardsPrefix, payload)
      }

      case class creditCard(id: Int) {
        val creditCardPath = s"${creditCards.creditCardsPrefix}/$id"

        def toggleDefault(payload: ToggleDefaultCreditCard): HttpResponse =
          POST(s"$creditCardPath/default", payload)

        def edit(payload: EditCreditCard): HttpResponse =
          PATCH(creditCardPath, payload)

        def delete(): HttpResponse =
          DELETE(creditCardPath)
      }

      object storeCredit {
        val storeCreditPrefix = s"$paymentPrefix/store-credit"

        def create(payload: CreateManualStoreCredit): HttpResponse =
          POST(storeCreditPrefix, payload)

        def totals(): HttpResponse =
          GET(s"$storeCreditPrefix/totals")
      }

      case class storeCredit(id: Int) {
        val storeCreditPath = s"${storeCredit.storeCreditPrefix}/$id"

        def convert(): HttpResponse =
          POST(s"$storeCreditPath/convert")
      }
    }
  }

  object activityTrailsApi {
    val activityTrailPrefix = s"$rootPrefix/trails"

    def appendActivity(dimension: String, objectId: Int, payload: AppendActivity): HttpResponse =
      POST(s"$activityTrailPrefix/$dimension/$objectId", payload)
  }

  object giftCardsApi {
    val giftCardsPrefix   = s"$rootPrefix/gift-cards"
    val customerGiftCards = s"$rootPrefix/customer-gift-cards"

    def create(payload: GiftCardCreateByCsr): HttpResponse =
      POST(giftCardsPrefix, payload)

    def createFromCustomer(payload: GiftCardCreatedByCustomer): HttpResponse =
      POST(customerGiftCards, payload)

    def createMultipleFromCustomer(payload: Seq[GiftCardCreatedByCustomer]): HttpResponse =
      POST(s"$customerGiftCards/bulk", payload)

    def createBulk(payload: GiftCardBulkCreateByCsr): HttpResponse =
      POST(s"$giftCardsPrefix/bulk", payload)

    def updateBulk(payload: GiftCardBulkUpdateStateByCsr): HttpResponse =
      PATCH(s"$giftCardsPrefix/bulk", payload)
  }

  case class giftCardsApi(code: String) {
    val giftCardPath = s"${giftCardsApi.giftCardsPrefix}/$code"

    def get(): HttpResponse =
      GET(giftCardPath)

    def update(payload: GiftCardUpdateStateByCsr): HttpResponse =
      PATCH(giftCardPath, payload)

    def transactions(): HttpResponse =
      GET(s"$giftCardPath/transactions")

    def convertToStoreCredit(customerId: Int): HttpResponse =
      POST(s"$giftCardPath/convert/$customerId")
  }

  object returnsApi {
    val returnsPrefix = s"$rootPrefix/returns"

    def create(payload: ReturnCreatePayload): HttpResponse =
      POST(returnsPrefix, payload)

    def get(): HttpResponse =
      GET(returnsPrefix)

    def getByCustomer(id: Int): HttpResponse =
      GET(s"$returnsPrefix/customer/$id")

    def getByOrder(ref: String): HttpResponse =
      GET(s"$returnsPrefix/order/$ref")

    object reasons {
      val requestPath = s"$returnsPrefix/reasons"

      def list(): HttpResponse = GET(requestPath)

      def add(payload: ReturnReasonPayload): HttpResponse = POST(requestPath, payload)

      def remove(id: Int) = DELETE(s"$requestPath/$id")
    }
  }

  case class returnsApi(refNum: String) { returns ⇒
    val requestPath = s"${returnsApi.returnsPrefix}/$refNum"

    def update(payload: ReturnUpdateStatePayload): HttpResponse =
      PATCH(requestPath, payload)

    def get(): HttpResponse =
      GET(requestPath)

    def getLock(): HttpResponse =
      GET(s"$requestPath/lock")

    def lock(): HttpResponse =
      POST(s"$requestPath/lock")

    def unlock(): HttpResponse =
      POST(s"$requestPath/unlock")

    def message(payload: ReturnMessageToCustomerPayload) =
      POST(s"$requestPath/message", payload)

    object lineItems {
      val requestPath = s"${returns.requestPath}/line-items"

      def add(payload: ReturnLineItemPayload): HttpResponse =
        POST(requestPath, payload)

      def remove(lineItemId: Int): HttpResponse =
        DELETE(s"$requestPath/$lineItemId")
    }

    object paymentMethods {
      val requestPath = s"${returns.requestPath}/payment-methods"

      def add(payload: ReturnPaymentPayload): HttpResponse =
        POST(requestPath, payload)

      def remove(paymentMethod: PaymentMethod.Type): HttpResponse =
        DELETE(s"$requestPath/${PaymentMethod.Type.show(paymentMethod)}")
    }
  }

  object ordersApi {
    val ordersPrefix = s"$rootPrefix/orders"

    def update(payload: BulkUpdateOrdersPayload): HttpResponse =
      PATCH(ordersPrefix, payload)

    def assign(payload: BulkAssignmentPayload[String]): HttpResponse =
      POST(s"$ordersPrefix/assignees", payload)

    def unassign(payload: BulkAssignmentPayload[String]): HttpResponse =
      POST(s"$ordersPrefix/assignees/delete", payload)
  }

  case class ordersApi(refNum: String) {
    val orderPath = s"${ordersApi.ordersPrefix}/$refNum"

    def update(payload: UpdateOrderPayload): HttpResponse =
      PATCH(orderPath, payload)

    def assign(payload: AssignmentPayload): HttpResponse =
      POST(s"$orderPath/assignees", payload)

    def unassign(adminId: Int): HttpResponse =
      DELETE(s"$orderPath/assignees/$adminId")

    def increaseRemorsePeriod(): HttpResponse =
      POST(s"$orderPath/increase-remorse-period")
  }

  object cartsApi {
    val cartsPrefix = s"$rootPrefix/carts"

    def create(payload: CreateCart): HttpResponse =
      POST(cartsPrefix, payload)
  }

  case class cartsApi(refNum: String) {

    val cartPath     = s"${cartsApi.cartsPrefix}/$refNum"
    val updateLIAttr = s"$cartPath/line-items/attributes"

    def updateCartLineItem(payload: Seq[UpdateOrderLineItemsPayload]): HttpResponse = {
      PATCH(updateLIAttr, payload)
    }

    def get(): HttpResponse =
      GET(cartPath)

    def lock(): HttpResponse =
      POST(s"$cartPath/lock")

    def unlock(): HttpResponse =
      POST(s"$cartPath/unlock")

    def checkout(): HttpResponse =
      POST(s"$cartPath/checkout")

    object coupon {
      val couponPrefix = s"$cartPath/coupon"

      def add(code: String): HttpResponse =
        POST(s"$couponPrefix/$code")

      def delete(): HttpResponse =
        DELETE(couponPrefix)
    }

    object shippingMethod {
      val shippingMethodPrefix = s"$cartPath/shipping-method"

      def update(payload: UpdateShippingMethod): HttpResponse =
        PATCH(shippingMethodPrefix, payload)

      def delete(): HttpResponse =
        DELETE(shippingMethodPrefix)
    }

    object shippingAddress {
      val shippingAddressPrefix = s"$cartPath/shipping-address"

      def create(payload: CreateAddressPayload): HttpResponse =
        POST(shippingAddressPrefix, payload)

      def update(payload: UpdateAddressPayload): HttpResponse =
        PATCH(shippingAddressPrefix, payload)

      def updateFromAddress(addressId: Int): HttpResponse =
        PATCH(s"$shippingAddressPrefix/$addressId")

      def delete(): HttpResponse =
        DELETE(shippingAddressPrefix)
    }

    object lineItems {
      val lineItemsPrefix = s"$cartPath/line-items"

      def add(payload: Seq[UpdateLineItemsPayload]): HttpResponse =
        POST(lineItemsPrefix, payload)

      def update(payload: Seq[UpdateLineItemsPayload]): HttpResponse =
        PATCH(lineItemsPrefix, payload)
    }

    object payments {
      val paymentPrefix = s"$cartPath/payment-methods"

      object creditCard {
        val creditCardPrefix = s"$paymentPrefix/credit-cards"

        def add(payload: CreditCardPayment): HttpResponse =
          POST(creditCardPrefix, payload)

        def delete(): HttpResponse =
          DELETE(creditCardPrefix)
      }

      object giftCard {
        val giftCardPrefix = s"$paymentPrefix/gift-cards"

        def add(payload: GiftCardPayment): HttpResponse =
          POST(giftCardPrefix, payload)

        def update(payload: GiftCardPayment): HttpResponse =
          PATCH(giftCardPrefix, payload)

        def delete(code: String): HttpResponse =
          DELETE(s"$giftCardPrefix/$code")
      }

      object storeCredit {
        val storeCreditPrefix = s"$paymentPrefix/store-credit"

        def add(payload: StoreCreditPayment): HttpResponse =
          POST(storeCreditPrefix, payload)

        def delete(): HttpResponse =
          DELETE(storeCreditPrefix)
      }
    }
  }

  object couponsApi {
    def couponsPrefix(implicit ctx: OC) = s"$rootPrefix/coupons/${ctx.name}"

    def create(payload: CreateCoupon)(implicit ctx: OC): HttpResponse =
      POST(couponsPrefix, payload)
  }

  case class couponsApi(formId: Int)(implicit ctx: OC) {
    val couponPath = s"${couponsApi.couponsPrefix}/$formId"

    def get(): HttpResponse =
      GET(couponPath)

    def archive(): HttpResponse =
      DELETE(couponPath)

    object codes {

      def generate(code: String): HttpResponse =
        POST(s"$rootPrefix/coupons/codes/generate/$formId/$code")
    }
  }

  object customerGroupsApi {
    val customerGroupsPrefix = s"$rootPrefix/groups"

    def get(): HttpResponse =
      GET(customerGroupsPrefix)

    def create(payload: CustomerDynamicGroupPayload): HttpResponse =
      POST(customerGroupsPrefix, payload)
  }

  case class customerGroupsApi(id: Int) {
    val customerGroupPath = s"${customerGroupsApi.customerGroupsPrefix}/$id"

    def get(): HttpResponse =
      GET(customerGroupPath)

    def update(payload: CustomerDynamicGroupPayload): HttpResponse =
      PATCH(customerGroupPath, payload)
  }

  case class genericTreesApi(name: String) {
    def genericTreePath(implicit ctx: OC) = s"$rootPrefix/tree/${ctx.name}/$name"

    def get(): HttpResponse =
      GET(genericTreePath)

    def create(payload: NodePayload): HttpResponse =
      POST(genericTreePath, payload)

    def createInPath(path: String, payload: NodePayload): HttpResponse =
      POST(s"$genericTreePath/$path", payload)

    def moveNode(payload: MoveNodePayload): HttpResponse =
      PATCH(genericTreePath, payload)

    def moveNodeInPath(path: String, payload: NodeValuesPayload): HttpResponse =
      PATCH(s"$genericTreePath/$path", payload)
  }

  object shippingMethodsApi {
    val shippingMethodsPrefix = s"$rootPrefix/shipping-methods"

    def forCart(refNum: String): HttpResponse =
      GET(s"$shippingMethodsPrefix/$refNum")
  }

  case object skusApi {
    val skusPrefix = s"$rootPrefix/skus"
    def skusPath(implicit ctx: OC) = s"$skusPrefix/${ctx.name}"

    def create(payload: SkuPayload)(implicit ctx: OC): HttpResponse =
      POST(skusPath, payload)
  }

  case class skusApi(code: String)(implicit val ctx: OC) {
    val skuPath = s"${skusApi.skusPath}/$code"

    def get(): HttpResponse =
      GET(skuPath)

    def update(payload: SkuPayload): HttpResponse =
      PATCH(skuPath, payload)

    def archive(): HttpResponse =
      DELETE(skuPath)

    object albums {
      val albumsPrefix = s"$skuPath/albums"

      def get(): HttpResponse =
        GET(albumsPrefix)

      def create(payload: AlbumPayload): HttpResponse =
        POST(albumsPrefix, payload)

      def update(payload: AlbumPayload): HttpResponse =
        PATCH(albumsPrefix, payload)
    }
  }

  object productsApi {
    def productsPrefix()(implicit ctx: OC) = s"$rootPrefix/products/${ctx.name}"

    def create(payload: CreateProductPayload)(implicit ctx: OC): HttpResponse =
      POST(productsPrefix, payload)

    def apply(formId: Int)(implicit ctx: OC): productsApi = productsApi(formId.toString)
  }

  case class productsApi(reference: String)(implicit ctx: OC) {
    val productPath = s"${productsApi.productsPrefix}/$reference"

    def get(): HttpResponse =
      GET(productPath)

    def update(payload: UpdateProductPayload): HttpResponse =
      PATCH(productPath, payload)

    def archive(): HttpResponse =
      DELETE(productPath)

    object albums {
      val albumsPrefix = s"$productPath/albums"

      def get(): HttpResponse =
        GET(albumsPrefix)

      def create(payload: AlbumPayload): HttpResponse =
        POST(albumsPrefix, payload)

      // Why not PATCH?
      def updatePosition(payload: UpdateAlbumPositionPayload): HttpResponse =
        POST(s"$albumsPrefix/position", payload)
    }

    object taxons {
      def get: HttpResponse =
        GET(s"$productPath/taxons")
    }
  }

  object storeAdminsApi {
    val storeAdminsPrefix = s"$rootPrefix/store-admins"

    def create(payload: CreateStoreAdminPayload): HttpResponse =
      POST(storeAdminsPrefix, payload)
  }

  case class storeAdminsApi(id: Int) {
    val storeAdminPath = s"${storeAdminsApi.storeAdminsPrefix}/$id"

    def get(): HttpResponse =
      GET(storeAdminPath)

    def update(payload: UpdateStoreAdminPayload): HttpResponse =
      PATCH(storeAdminPath, payload)

    def updateState(payload: StateChangeStoreAdminPayload): HttpResponse =
      PATCH(s"$storeAdminPath/state", payload)

    def delete(): HttpResponse =
      DELETE(storeAdminPath)
  }

  object variantsApi {
    def variantsPrefix()(implicit ctx: OC) = s"$rootPrefix/variants/${ctx.name}"

    def create(payload: VariantPayload)(implicit ctx: OC): HttpResponse =
      POST(variantsPrefix, payload)
  }

  case class variantsApi(formId: Int)(implicit ctx: OC) {
    val variantPath = s"${variantsApi.variantsPrefix}/$formId"

    def get()(implicit ctx: OC): HttpResponse =
      GET(variantPath)

    def update(payload: VariantPayload)(implicit ctx: OC): HttpResponse =
      PATCH(variantPath, payload)

    def createValues(payload: VariantValuePayload)(implicit ctx: OC): HttpResponse =
      POST(s"$variantPath/values", payload)
  }

  object albumsApi {
    def albumsPrefix()(implicit ctx: OC) = s"$rootPrefix/albums/${ctx.name}"

    def create(payload: AlbumPayload)(implicit ctx: OC): HttpResponse =
      POST(albumsPrefix, payload)
  }

  case class albumsApi(formId: Int)(implicit val ctx: OC) {
    val albumPath = s"${albumsApi.albumsPrefix}/$formId"

    def get(): HttpResponse =
      GET(albumPath)

    def update(payload: AlbumPayload): HttpResponse =
      PATCH(albumPath, payload)

    def delete(): HttpResponse =
      DELETE(albumPath)
  }

  object saveForLaterApi {
    val saveForLaterPrefix = s"$rootPrefix/save-for-later"

    def delete(id: Int): HttpResponse =
      DELETE(s"$saveForLaterPrefix/$id")
  }

  case class saveForLaterApi(customerId: Int) {
    val saveForLaterPrefix = s"${saveForLaterApi.saveForLaterPrefix}/$customerId"

    def get(): HttpResponse =
      GET(saveForLaterPrefix)

    def create(sku: String): HttpResponse =
      POST(s"$saveForLaterPrefix/$sku")
  }

  object sharedSearchApi {
    val sharedSearchPrefix = s"$rootPrefix/shared-search"

    def scope(scope: String): HttpResponse =
      GET(s"$sharedSearchPrefix?scope=$scope")

    def create(payload: SharedSearchPayload): HttpResponse =
      POST(sharedSearchPrefix, payload)

    def createFromQuery(query: String): HttpResponse =
      POST(sharedSearchPrefix, query)
  }

  case class sharedSearchApi(code: String) {
    val sharedSearchPath = s"${sharedSearchApi.sharedSearchPrefix}/$code"

    def get(): HttpResponse =
      GET(sharedSearchPath)

    def create(payload: SharedSearchPayload): HttpResponse =
      POST(sharedSearchPath, payload)

    def update(payload: SharedSearchPayload): HttpResponse =
      PATCH(sharedSearchPath, payload)

    def updateFromQuery(query: String): HttpResponse =
      PATCH(sharedSearchPath, query)

    def delete(): HttpResponse =
      DELETE(sharedSearchPath)

    def associate(payload: SharedSearchAssociationPayload): HttpResponse =
      POST(s"$sharedSearchPath/associate", payload)

    def associates(): HttpResponse =
      GET(s"$sharedSearchPath/associates")

    def unassociate(adminId: Int): HttpResponse =
      DELETE(s"$sharedSearchPath/associate/$adminId")
  }

  object promotionsApi {
    def promotionsPrefix(implicit ctx: OC) = s"$rootPrefix/promotions/${ctx.name}"

    def create(payload: CreatePromotion)(implicit ctx: OC): HttpResponse =
      POST(promotionsPrefix, payload)
  }

  case class promotionsApi(formId: Int)(implicit ctx: OC) {
    val promotionPath = s"${promotionsApi.promotionsPrefix}/$formId"

    def delete(): HttpResponse =
      DELETE(promotionPath)

    def update(payload: UpdatePromotion): HttpResponse =
      PATCH(promotionPath, payload)
  }

  object categoriesApi {
    def categoriesPrefix()(implicit ctx: OC) = s"$rootPrefix/categories/${ctx.name}"

    def create(payload: CreateFullCategory)(implicit ctx: OC): HttpResponse =
      POST(categoriesPrefix, payload)
  }

  case class categoriesApi(formId: Int)(implicit ctx: OC) {
    val categoryPath = s"${categoriesApi.categoriesPrefix}/$formId"

    def get(): HttpResponse =
      GET(categoryPath)

    def update(payload: UpdateFullCategory): HttpResponse =
      PATCH(categoryPath, payload)

    def form(): HttpResponse =
      GET(s"$rootPrefix/categories/$formId/form")

    def shadow(): HttpResponse =
      GET(s"$categoryPath/shadow")

    def baked(): HttpResponse =
      GET(s"$categoryPath/baked")

  }

  case object taxonomiesApi {
    def create(payload: CreateTaxonomyPayload)(implicit ctx: OC) =
      POST(s"v1/taxonomies/${ctx.name}", payload)
  }

  case class taxonomiesApi(taxonomyId: Int)(implicit ctx: OC) {
    def update(payload: UpdateTaxonomyPayload) =
      PATCH(s"v1/taxonomies/${ctx.name}/$taxonomyId", payload)
    def delete = DELETE(s"v1/taxonomies/${ctx.name}/$taxonomyId")
    def get    = GET(s"v1/taxonomies/${ctx.name}/$taxonomyId")
    def createTaxon(payload: CreateTaxonPayload) =
      POST(s"v1/taxonomies/${ctx.name}/$taxonomyId", payload)
  }

  case class taxonsApi(taxonId: Int)(implicit ctx: OC) {
    def get = GET(s"v1/taxons/${ctx.name}/$taxonId")
    def update(payload: UpdateTaxonPayload) =
      PATCH(s"v1/taxons/${ctx.name}/$taxonId", payload)
    def delete = DELETE(s"v1/taxons/${ctx.name}/$taxonId")

    def assignProduct(productFormId: ObjectForm#Id)(implicit ctx: OC): HttpResponse =
      PATCH(s"v1/taxons/${ctx.name}/$taxonId/product/$productFormId")

    def unassignProduct(productFormId: ObjectForm#Id)(implicit ctx: OC): HttpResponse =
      DELETE(s"v1/taxons/${ctx.name}/$taxonId/product/$productFormId")
  }

  object notesApi {
    val notesPrefix = s"$rootPrefix/notes"

    case class customer(id: Int)    extends notesApiBase[Int]    { val prefix = "customer"     }
    case class storeAdmin(id: Int)  extends notesApiBase[Int]    { val prefix = "store-admins" }
    case class order(id: String)    extends notesApiBase[String] { val prefix = "order"        }
    case class giftCard(id: String) extends notesApiBase[String] { val prefix = "gift-card"    }
    case class returns(id: String)  extends notesApiBase[String] { val prefix = "return"       }

    trait notesApiBase[A] {
      def id: A
      def prefix: String

      lazy val path = s"$notesPrefix/$prefix/$id"

      def get(): HttpResponse =
        GET(path)

      def create(payload: CreateNote): HttpResponse =
        POST(path, payload)

      case class note(id: Int) {
        val notePath = s"$path/$id"

        def update(payload: UpdateNote): HttpResponse =
          PATCH(notePath, payload)

        def delete(): HttpResponse =
          DELETE(notePath)
      }
    }
  }

  object storeCreditsApi {
    val storeCreditsPrefix = s"$rootPrefix/store-credits"

    def update(payload: StoreCreditBulkUpdateStateByCsr): HttpResponse =
      PATCH(storeCreditsPrefix, payload)
  }

  case class storeCreditsApi(id: Int) {
    val storeCreditPath = s"${storeCreditsApi.storeCreditsPrefix}/$id"

    def update(payload: StoreCreditUpdateStateByCsr): HttpResponse =
      PATCH(storeCreditPath, payload)
  }

  object notificationsApi {
    val notificationsPrefix = s"$rootPrefix/notifications"

    def create(payload: CreateNotification): HttpResponse =
      POST(notificationsPrefix, payload)

    def updateLastSeen(activityId: Int): HttpResponse =
      POST(s"$notificationsPrefix/last-seen/$activityId")
  }
}
