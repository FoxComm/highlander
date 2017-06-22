package testutils.apis

import akka.http.scaladsl.model.HttpResponse

import cats.implicits._
import objectframework.models.ObjectForm
import phoenix.models.payment.PaymentMethod
import phoenix.payloads.ActivityTrailPayloads._
import phoenix.payloads.AddressPayloads._
import phoenix.payloads.AssignmentPayloads._
import phoenix.payloads.CartPayloads._
import phoenix.payloads.CatalogPayloads._
import phoenix.payloads.CategoryPayloads._
import phoenix.payloads.CouponPayloads._
import phoenix.payloads.CustomerGroupPayloads._
import phoenix.payloads.CustomerPayloads._
import phoenix.payloads.GenericTreePayloads._
import phoenix.payloads.GiftCardPayloads._
import phoenix.payloads.ImagePayloads._
import phoenix.payloads.LineItemPayloads._
import phoenix.payloads.NotePayloads._
import phoenix.payloads.OrderPayloads._
import phoenix.payloads.PaymentPayloads._
import phoenix.payloads.ProductPayloads._
import phoenix.payloads.ProductReviewPayloads._
import phoenix.payloads.PromotionPayloads._
import phoenix.payloads.ReturnPayloads._
import phoenix.payloads.SharedSearchPayloads._
import phoenix.payloads.SkuPayloads._
import phoenix.payloads.StoreAdminPayloads._
import phoenix.payloads.StoreCreditPayloads._
import phoenix.payloads.TaxonPayloads._
import phoenix.payloads.TaxonomyPayloads._
import phoenix.payloads.UserPayloads._
import phoenix.payloads.VariantPayloads._
import phoenix.payloads._
import phoenix.utils.aliases.OC
import testutils._

/*
 * prefix → string literal prefix, e.g. "/customers"
 * path   → "compiled" path, e.g. "/customers/1"
 */
trait PhoenixAdminApi extends HttpSupport { self: FoxSuite ⇒

  private val rootPrefix = "v1"

  object customersApi {
    val customersPrefix = s"$rootPrefix/customers"

    def create(payload: CreateCustomerPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(customersPrefix, payload, aa.jwtCookie.some)
  }

  case class customersApi(id: Int) {
    val customerPath = s"${customersApi.customersPrefix}/$id"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(customerPath, aa.jwtCookie.some)

    def update(payload: UpdateCustomerPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(s"$customerPath", payload, aa.jwtCookie.some)

    def activate(payload: ActivateCustomerPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$customerPath/activate", payload, aa.jwtCookie.some)

    def disable(payload: ToggleUserDisabled)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$customerPath/disable", payload, aa.jwtCookie.some)

    def blacklist(payload: ToggleUserBlacklisted)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$customerPath/blacklist", payload, aa.jwtCookie.some)

    def cart()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$customerPath/cart", aa.jwtCookie.some)

    object addresses {
      val addressesPrefix = s"$customerPath/addresses"

      def get()(implicit aa: TestAdminAuth): HttpResponse =
        GET(addressesPrefix, aa.jwtCookie.some)

      def create(payload: CreateAddressPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(addressesPrefix, payload, aa.jwtCookie.some)

      def unsetDefault()(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(s"$addressesPrefix/default", aa.jwtCookie.some)
    }

    case class address(id: Int) {
      val addressPath = s"${addresses.addressesPrefix}/$id"

      def get()(implicit aa: TestAdminAuth): HttpResponse =
        GET(addressPath, aa.jwtCookie.some)

      def edit(payload: CreateAddressPayload)(implicit aa: TestAdminAuth): HttpResponse =
        PATCH(addressPath, payload, aa.jwtCookie.some)

      def delete()(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(addressPath, aa.jwtCookie.some)

      def setDefault()(implicit aa: TestAdminAuth): HttpResponse =
        POST(s"$addressPath/default", aa.jwtCookie.some)
    }

    object payments {
      val paymentPrefix = s"$customerPath/payment-methods"

      object creditCards {
        val creditCardsPrefix = s"$paymentPrefix/credit-cards"

        def get()(implicit aa: TestAdminAuth): HttpResponse =
          GET(creditCardsPrefix, aa.jwtCookie.some)

        def create(payload: CreateCreditCardFromTokenPayload)(implicit aa: TestAdminAuth): HttpResponse =
          POST(creditCardsPrefix, payload, aa.jwtCookie.some)

        def unsetDefault()(implicit aa: TestAdminAuth): HttpResponse =
          DELETE(s"$creditCardsPrefix/default", aa.jwtCookie.some)
      }

      case class creditCard(id: Int) {
        val creditCardPath = s"${creditCards.creditCardsPrefix}/$id"

        def setDefault()(implicit aa: TestAdminAuth): HttpResponse =
          POST(s"$creditCardPath/default", aa.jwtCookie.some)

        def edit(payload: EditCreditCard)(implicit aa: TestAdminAuth): HttpResponse =
          PATCH(creditCardPath, payload, aa.jwtCookie.some)

        def delete()(implicit aa: TestAdminAuth): HttpResponse =
          DELETE(creditCardPath, aa.jwtCookie.some)
      }

      object storeCredit {
        val storeCreditPrefix = s"$paymentPrefix/store-credit"

        def create(payload: CreateManualStoreCredit)(implicit aa: TestAdminAuth): HttpResponse =
          POST(storeCreditPrefix, payload, aa.jwtCookie.some)

        def totals()(implicit aa: TestAdminAuth): HttpResponse =
          GET(s"$storeCreditPrefix/totals", aa.jwtCookie.some)
      }

      case class storeCredit(id: Int) {
        val storeCreditPath = s"${storeCredit.storeCreditPrefix}/$id"

        def convert()(implicit aa: TestAdminAuth): HttpResponse =
          POST(s"$storeCreditPath/convert", aa.jwtCookie.some)
      }
    }

    object groups {
      val customerGroupsPath = s"$customerPath/customer-groups"

      def syncGroups(payload: AddCustomerToGroups)(implicit aa: TestAdminAuth): HttpResponse =
        POST(customerGroupsPath, payload, aa.jwtCookie.some)
    }
  }

  object giftCardsApi {
    val giftCardsPrefix   = s"$rootPrefix/gift-cards"
    val customerGiftCards = s"$rootPrefix/customer-gift-cards"

    def create(payload: GiftCardCreateByCsr)(implicit aa: TestAdminAuth): HttpResponse =
      POST(giftCardsPrefix, payload, aa.jwtCookie.some)

    def createFromCustomer(payload: GiftCardCreatedByCustomer)(implicit aa: TestAdminAuth): HttpResponse =
      POST(customerGiftCards, payload, aa.jwtCookie.some)

    def createMultipleFromCustomer(payload: Seq[GiftCardCreatedByCustomer])(
        implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$customerGiftCards/bulk", payload, aa.jwtCookie.some)

    def createBulk(payload: GiftCardBulkCreateByCsr)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$giftCardsPrefix/bulk", payload, aa.jwtCookie.some)

    def updateBulk(payload: GiftCardBulkUpdateStateByCsr)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(s"$giftCardsPrefix/bulk", payload, aa.jwtCookie.some)
  }

  case class giftCardsApi(code: String) {
    val giftCardPath = s"${giftCardsApi.giftCardsPrefix}/$code"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(giftCardPath, aa.jwtCookie.some)

    def update(payload: GiftCardUpdateStateByCsr)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(giftCardPath, payload, aa.jwtCookie.some)

    def transactions()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$giftCardPath/transactions", aa.jwtCookie.some)

    def convertToStoreCredit(customerId: Int)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$giftCardPath/convert/$customerId", aa.jwtCookie.some)
  }

  object returnsApi {
    val returnsPrefix = s"$rootPrefix/returns"

    def create(payload: ReturnCreatePayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(returnsPrefix, payload, aa.jwtCookie.some)

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(returnsPrefix, aa.jwtCookie.some)

    def getByCustomer(id: Int)(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$returnsPrefix/customer/$id", aa.jwtCookie.some)

    def getByOrder(ref: String)(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$returnsPrefix/order/$ref", aa.jwtCookie.some)

    object reasons {
      val requestPath = s"$returnsPrefix/reasons"

      def list()(implicit aa: TestAdminAuth): HttpResponse =
        GET(requestPath, aa.jwtCookie.some)

      def add(payload: ReturnReasonPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(requestPath, payload, aa.jwtCookie.some)

      def remove(id: Int)(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(s"$requestPath/$id", aa.jwtCookie.some)
    }
  }

  case class returnsApi(refNum: String) { returns ⇒
    val requestPath = s"${returnsApi.returnsPrefix}/$refNum"

    def update(payload: ReturnUpdateStatePayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(requestPath, payload, aa.jwtCookie.some)

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(requestPath, aa.jwtCookie.some)

    def getLock()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$requestPath/lock", aa.jwtCookie.some)

    def lock()(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$requestPath/lock", aa.jwtCookie.some)

    def unlock()(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$requestPath/unlock", aa.jwtCookie.some)

    def message(payload: ReturnMessageToCustomerPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$requestPath/message", payload, aa.jwtCookie.some)

    object lineItems {
      val requestPath = s"${returns.requestPath}/line-items"

      def addOrReplace(payload: List[ReturnSkuLineItemPayload])(implicit aa: TestAdminAuth): HttpResponse =
        POST(s"$requestPath/skus", payload, aa.jwtCookie.some)

      def add(payload: ReturnLineItemPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(requestPath, payload, aa.jwtCookie.some)

      def remove(lineItemId: Int)(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(s"$requestPath/$lineItemId", aa.jwtCookie.some)
    }

    object paymentMethods {
      val requestPath = s"${returns.requestPath}/payment-methods"

      def addOrReplace(payload: ReturnPaymentsPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(requestPath, payload, aa.jwtCookie.some)

      def add(paymentMethod: PaymentMethod.Type, payload: ReturnPaymentPayload)(
          implicit aa: TestAdminAuth): HttpResponse =
        POST(s"$requestPath/${PaymentMethod.Type.show(paymentMethod)}", payload, aa.jwtCookie.some)

      def remove(paymentMethod: PaymentMethod.Type)(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(s"$requestPath/${PaymentMethod.Type.show(paymentMethod)}", aa.jwtCookie.some)
    }
  }

  object ordersApi {
    val ordersPrefix = s"$rootPrefix/orders"

    def update(payload: BulkUpdateOrdersPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(ordersPrefix, payload, aa.jwtCookie.some)

    def assign(payload: BulkAssignmentPayload[String])(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$ordersPrefix/assignees", payload, aa.jwtCookie.some)

    def unassign(payload: BulkAssignmentPayload[String])(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$ordersPrefix/assignees/delete", payload, aa.jwtCookie.some)
  }

  case class ordersApi(refNum: String) {
    val orderPath = s"${ordersApi.ordersPrefix}/$refNum"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(orderPath, aa.jwtCookie.some)

    def update(payload: UpdateOrderPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(orderPath, payload, aa.jwtCookie.some)

    def assign(payload: AssignmentPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$orderPath/assignees", payload, aa.jwtCookie.some)

    def unassign(adminId: Int)(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"$orderPath/assignees/$adminId", aa.jwtCookie.some)

    def increaseRemorsePeriod()(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$orderPath/increase-remorse-period", aa.jwtCookie.some)
  }

  object cartsApi {
    val cartsPrefix = s"$rootPrefix/carts"

    def create(payload: CreateCart)(implicit aa: TestAdminAuth): HttpResponse =
      POST(cartsPrefix, payload, aa.jwtCookie.some)
  }

  case class cartsApi(refNum: String) {

    val cartPath     = s"${cartsApi.cartsPrefix}/$refNum"
    val updateLIAttr = s"$cartPath/line-items/attributes"

    def updateCartLineItem(payload: Seq[UpdateOrderLineItemsPayload])(
        implicit aa: TestAdminAuth): HttpResponse =
      PATCH(updateLIAttr, payload, aa.jwtCookie.some)

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(cartPath, aa.jwtCookie.some)

    def checkout()(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$cartPath/checkout", aa.jwtCookie.some)

    object coupon {
      val couponPrefix = s"$cartPath/coupon"

      def add(code: String)(implicit aa: TestAdminAuth): HttpResponse =
        POST(s"$couponPrefix/$code", aa.jwtCookie.some)

      def delete()(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(couponPrefix, aa.jwtCookie.some)
    }

    object shippingMethod {
      val shippingMethodPrefix = s"$cartPath/shipping-method"

      def update(payload: UpdateShippingMethod)(implicit aa: TestAdminAuth): HttpResponse =
        PATCH(shippingMethodPrefix, payload, aa.jwtCookie.some)

      def delete()(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(shippingMethodPrefix, aa.jwtCookie.some)
    }

    object shippingAddress {
      val shippingAddressPrefix = s"$cartPath/shipping-address"

      def create(payload: CreateAddressPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(shippingAddressPrefix, payload, aa.jwtCookie.some)

      def update(payload: UpdateAddressPayload)(implicit aa: TestAdminAuth): HttpResponse =
        PATCH(shippingAddressPrefix, payload, aa.jwtCookie.some)

      def updateFromAddress(addressId: Int)(implicit aa: TestAdminAuth): HttpResponse =
        PATCH(s"$shippingAddressPrefix/$addressId", aa.jwtCookie.some)

      def delete()(implicit aa: TestAdminAuth): HttpResponse =
        DELETE(shippingAddressPrefix, aa.jwtCookie.some)
    }

    object lineItems {
      val lineItemsPrefix = s"$cartPath/line-items"

      def add(payload: Seq[UpdateLineItemsPayload])(implicit aa: TestAdminAuth): HttpResponse =
        POST(lineItemsPrefix, payload, aa.jwtCookie.some)

      def update(payload: Seq[UpdateLineItemsPayload])(implicit aa: TestAdminAuth): HttpResponse =
        PATCH(lineItemsPrefix, payload, aa.jwtCookie.some)
    }

    object payments {
      val paymentPrefix = s"$cartPath/payment-methods"

      object applePay {
        val applePayPrefix = s"$paymentPrefix/apple-pay"

        def add(payload: CreateApplePayPayment)(implicit aa: TestAdminAuth): HttpResponse =
          POST(applePayPrefix, payload, aa.jwtCookie.some)
      }

      object creditCard {
        val creditCardPrefix = s"$paymentPrefix/credit-cards"

        def add(payload: CreditCardPayment)(implicit aa: TestAdminAuth): HttpResponse =
          POST(creditCardPrefix, payload, aa.jwtCookie.some)

        def delete()(implicit aa: TestAdminAuth): HttpResponse =
          DELETE(creditCardPrefix, aa.jwtCookie.some)
      }

      object giftCard {
        val giftCardPrefix = s"$paymentPrefix/gift-cards"

        def add(payload: GiftCardPayment)(implicit aa: TestAdminAuth): HttpResponse =
          POST(giftCardPrefix, payload, aa.jwtCookie.some)

        def update(payload: GiftCardPayment)(implicit aa: TestAdminAuth): HttpResponse =
          PATCH(giftCardPrefix, payload, aa.jwtCookie.some)

        def delete(code: String)(implicit aa: TestAdminAuth): HttpResponse =
          DELETE(s"$giftCardPrefix/$code", aa.jwtCookie.some)
      }

      object storeCredit {
        val storeCreditPrefix = s"$paymentPrefix/store-credit"

        def add(payload: StoreCreditPayment)(implicit aa: TestAdminAuth): HttpResponse =
          POST(storeCreditPrefix, payload, aa.jwtCookie.some)

        def delete()(implicit aa: TestAdminAuth): HttpResponse =
          DELETE(storeCreditPrefix, aa.jwtCookie.some)
      }
    }
  }

  object couponsApi {
    def couponsPrefix(implicit ctx: OC) = s"$rootPrefix/coupons/${ctx.name}"

    def create(payload: CreateCoupon)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(couponsPrefix, payload, aa.jwtCookie.some)
  }

  case class couponsApi(formId: Int)(implicit ctx: OC) {
    val couponPath = s"${couponsApi.couponsPrefix}/$formId"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(couponPath, aa.jwtCookie.some)

    def archive()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(couponPath, aa.jwtCookie.some)

    object codes {

      def generate(code: String)(implicit aa: TestAdminAuth): HttpResponse =
        POST(s"$rootPrefix/coupons/codes/generate/$formId/$code", aa.jwtCookie.some)
    }
  }

  object customerGroupsApi {
    val customerGroupsPrefix = s"$rootPrefix/customer-groups"

    def create(payload: CustomerGroupPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(customerGroupsPrefix, payload, aa.jwtCookie.some)
  }

  object customerGroupTemplateApi {
    val customerGroupTemplatePrefix = s"$rootPrefix/customer-groups/templates"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(customerGroupTemplatePrefix, aa.jwtCookie.some)
  }

  case class customerGroupsMembersServiceApi(groupId: Int) {
    val customerGroupMembersPrefix = s"$rootPrefix/service/customer-groups/$groupId"

    def syncCustomers(payload: CustomerGroupMemberServiceSyncPayload)(
        implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$customerGroupMembersPrefix/customers", payload, aa.jwtCookie.some)
  }

  case class customerGroupsMembersApi(groupId: Int) {
    val customerGroupMembersPrefix = s"$rootPrefix/customer-groups/$groupId/customers"

    def syncCustomers(payload: CustomerGroupMemberSyncPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$customerGroupMembersPrefix", payload, aa.jwtCookie.some)
  }

  case class customerGroupsApi(id: Int) {
    val customerGroupPath = s"${customerGroupsApi.customerGroupsPrefix}/$id"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(customerGroupPath, aa.jwtCookie.some)

    def update(payload: CustomerGroupPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(customerGroupPath, payload, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(customerGroupPath, aa.jwtCookie.some)
  }

  case class genericTreesApi(name: String) {
    def genericTreePath(implicit ctx: OC) = s"$rootPrefix/tree/${ctx.name}/$name"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(genericTreePath, aa.jwtCookie.some)

    def create(payload: NodePayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(genericTreePath, payload, aa.jwtCookie.some)

    def createInPath(path: String, payload: NodePayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$genericTreePath/$path", payload, aa.jwtCookie.some)

    def moveNode(payload: MoveNodePayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(genericTreePath, payload, aa.jwtCookie.some)

    def moveNodeInPath(path: String, payload: NodeValuesPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(s"$genericTreePath/$path", payload, aa.jwtCookie.some)
  }

  object shippingMethodsApi {
    val shippingMethodsPrefix = s"$rootPrefix/shipping-methods"

    def active()(implicit aa: TestAdminAuth): HttpResponse =
      GET(shippingMethodsPrefix, aa.jwtCookie.some)

    def forCart(refNum: String)(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$shippingMethodsPrefix/$refNum", aa.jwtCookie.some)

    def getDefault()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$shippingMethodsPrefix/default", aa.jwtCookie.some)

    def unsetDefault()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"$shippingMethodsPrefix/default", aa.jwtCookie.some)
  }

  case class shippingMethodsApi(id: Int) {
    def setDefault()(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"${shippingMethodsApi.shippingMethodsPrefix}/$id/default", aa.jwtCookie.some)
  }

  case object skusApi {
    val skusPrefix                 = s"$rootPrefix/skus"
    def skusPath(implicit ctx: OC) = s"$skusPrefix/${ctx.name}"

    def create(payload: SkuPayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(skusPath, payload, aa.jwtCookie.some)
  }

  case class skusApi(code: String)(implicit val ctx: OC) {
    val skuPath = s"${skusApi.skusPath}/$code"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(skuPath, aa.jwtCookie.some)

    def update(payload: SkuPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(skuPath, payload, aa.jwtCookie.some)

    def archive()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(skuPath, aa.jwtCookie.some)

    object albums {
      val albumsPrefix = s"$skuPath/albums"

      def get()(implicit aa: TestAdminAuth): HttpResponse =
        GET(albumsPrefix, aa.jwtCookie.some)

      def create(payload: AlbumPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(albumsPrefix, payload, aa.jwtCookie.some)

      def update(payload: AlbumPayload)(implicit aa: TestAdminAuth): HttpResponse =
        PATCH(albumsPrefix, payload, aa.jwtCookie.some)
    }
  }

  object productsApi {
    def productsPrefix()(implicit ctx: OC) = s"$rootPrefix/products/${ctx.name}"

    def create(payload: CreateProductPayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(productsPrefix, payload, aa.jwtCookie.some)

    def apply(formId: Int)(implicit ctx: OC): productsApi =
      productsApi(formId.toString)
  }

  case class productsApi(reference: String)(implicit ctx: OC) {
    val productPath = s"${productsApi.productsPrefix}/$reference"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(productPath, aa.jwtCookie.some)

    def update(payload: UpdateProductPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(productPath, payload, aa.jwtCookie.some)

    def archive()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(productPath, aa.jwtCookie.some)

    object albums {
      val albumsPrefix = s"$productPath/albums"

      def get()(implicit aa: TestAdminAuth): HttpResponse =
        GET(albumsPrefix, aa.jwtCookie.some)

      def create(payload: AlbumPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(albumsPrefix, payload, aa.jwtCookie.some)

      // Why not PATCH?
      def updatePosition(payload: UpdateAlbumPositionPayload)(implicit aa: TestAdminAuth): HttpResponse =
        POST(s"$albumsPrefix/position", payload, aa.jwtCookie.some)
    }

    object taxons {
      def get()(implicit aa: TestAdminAuth): HttpResponse =
        GET(s"$productPath/taxons", aa.jwtCookie.some)
    }
  }

  object storeAdminsApi {
    val storeAdminsPrefix = s"$rootPrefix/store-admins"

    def create(payload: CreateStoreAdminPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(storeAdminsPrefix, payload, aa.jwtCookie.some)
  }

  case class storeAdminsApi(id: Int) {
    val storeAdminPath = s"${storeAdminsApi.storeAdminsPrefix}/$id"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(storeAdminPath, aa.jwtCookie.some)

    def update(payload: UpdateStoreAdminPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(storeAdminPath, payload, aa.jwtCookie.some)

    def updateState(payload: StateChangeStoreAdminPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(s"$storeAdminPath/state", payload, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(storeAdminPath, aa.jwtCookie.some)
  }

  object variantsApi {
    def variantsPrefix()(implicit ctx: OC) = s"$rootPrefix/variants/${ctx.name}"

    def create(payload: VariantPayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(variantsPrefix, payload, aa.jwtCookie.some)
  }

  case class variantsApi(formId: Int)(implicit ctx: OC) {
    val variantPath = s"${variantsApi.variantsPrefix}/$formId"

    def get()(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      GET(variantPath, aa.jwtCookie.some)

    def update(payload: VariantPayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      PATCH(variantPath, payload, aa.jwtCookie.some)

    def createValues(payload: VariantValuePayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(s"$variantPath/values", payload, aa.jwtCookie.some)
  }

  object albumsApi {
    def albumsPrefix()(implicit ctx: OC) = s"$rootPrefix/albums/${ctx.name}"

    def create(payload: AlbumPayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(albumsPrefix, payload, aa.jwtCookie.some)
  }

  case class albumsApi(formId: Int)(implicit val ctx: OC) {
    val albumPath = s"${albumsApi.albumsPrefix}/$formId"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(albumPath, aa.jwtCookie.some)

    def update(payload: AlbumPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(albumPath, payload, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(albumPath, aa.jwtCookie.some)

    def uploadImageByUrl(payload: ImagePayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(s"$albumPath/images/by-url", payload, aa.jwtCookie.some)
  }

  object saveForLaterApi {
    val saveForLaterPrefix = s"$rootPrefix/save-for-later"

    def delete(id: Int)(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"$saveForLaterPrefix/$id", aa.jwtCookie.some)
  }

  case class saveForLaterApi(customerId: Int) {
    val saveForLaterPrefix = s"${saveForLaterApi.saveForLaterPrefix}/$customerId"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(saveForLaterPrefix, aa.jwtCookie.some)

    def create(sku: String)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$saveForLaterPrefix/$sku", aa.jwtCookie.some)
  }

  object sharedSearchApi {
    val sharedSearchPrefix = s"$rootPrefix/shared-search"

    def scope(scope: String)(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$sharedSearchPrefix?scope=$scope", aa.jwtCookie.some)

    def create(payload: SharedSearchPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(sharedSearchPrefix, payload, aa.jwtCookie.some)

    def createFromQuery(query: String)(implicit aa: TestAdminAuth): HttpResponse =
      POST(sharedSearchPrefix, query, aa.jwtCookie.some)
  }

  case class sharedSearchApi(code: String) {
    val sharedSearchPath = s"${sharedSearchApi.sharedSearchPrefix}/$code"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(sharedSearchPath, aa.jwtCookie.some)

    def create(payload: SharedSearchPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(sharedSearchPath, payload, aa.jwtCookie.some)

    def update(payload: SharedSearchPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(sharedSearchPath, payload, aa.jwtCookie.some)

    def updateFromQuery(query: String)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(sharedSearchPath, query, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(sharedSearchPath, aa.jwtCookie.some)

    def associate(payload: SharedSearchAssociationPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$sharedSearchPath/associate", payload, aa.jwtCookie.some)

    def associates()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$sharedSearchPath/associates", aa.jwtCookie.some)

    def unassociate(adminId: Int)(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"$sharedSearchPath/associate/$adminId", aa.jwtCookie.some)
  }

  object promotionsApi {
    def promotionsPrefix(implicit ctx: OC) = s"$rootPrefix/promotions/${ctx.name}"

    def create(payload: CreatePromotion)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(promotionsPrefix, payload, aa.jwtCookie.some)
  }

  case class promotionsApi(formId: Int)(implicit ctx: OC) {
    val promotionPath = s"${promotionsApi.promotionsPrefix}/$formId"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(promotionPath, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(promotionPath, aa.jwtCookie.some)

    def update(payload: UpdatePromotion)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(promotionPath, payload, aa.jwtCookie.some)
  }

  object categoriesApi {
    def categoriesPrefix()(implicit ctx: OC) = s"$rootPrefix/categories/${ctx.name}"

    def create(payload: CreateFullCategory)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(categoriesPrefix, payload, aa.jwtCookie.some)
  }

  case class categoriesApi(formId: Int)(implicit ctx: OC) {
    val categoryPath = s"${categoriesApi.categoriesPrefix}/$formId"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(categoryPath, aa.jwtCookie.some)

    def update(payload: UpdateFullCategory)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(categoryPath, payload, aa.jwtCookie.some)

    def form()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$rootPrefix/categories/$formId/form", aa.jwtCookie.some)

    def shadow()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$categoryPath/shadow", aa.jwtCookie.some)

    def baked()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"$categoryPath/baked", aa.jwtCookie.some)

  }

  case object taxonomiesApi {
    def create(payload: CreateTaxonomyPayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(s"v1/taxonomies/${ctx.name}", payload, aa.jwtCookie.some)
  }

  case class taxonomiesApi(taxonomyId: Int)(implicit ctx: OC) {
    def update(payload: UpdateTaxonomyPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(s"v1/taxonomies/${ctx.name}/$taxonomyId", payload, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"v1/taxonomies/${ctx.name}/$taxonomyId", aa.jwtCookie.some)

    def get()(implicit aa: TestAdminAuth) =
      GET(s"v1/taxonomies/${ctx.name}/$taxonomyId", aa.jwtCookie.some)

    def createTaxon(payload: CreateTaxonPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"v1/taxonomies/${ctx.name}/$taxonomyId/taxons", payload, aa.jwtCookie.some)
  }

  case class taxonsApi(taxonId: Int)(implicit ctx: OC) {

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"v1/taxons/${ctx.name}/$taxonId", aa.jwtCookie.some)

    def update(payload: UpdateTaxonPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(s"v1/taxons/${ctx.name}/$taxonId", payload, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"v1/taxons/${ctx.name}/$taxonId", aa.jwtCookie.some)

    def assignProduct(productFormId: ObjectForm#Id)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      PATCH(s"v1/taxons/${ctx.name}/$taxonId/product/$productFormId", aa.jwtCookie.some)

    def unassignProduct(productFormId: ObjectForm#Id)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      DELETE(s"v1/taxons/${ctx.name}/$taxonId/product/$productFormId", aa.jwtCookie.some)
  }

  object productReviewApi {
    def create(payload: CreateProductReviewPayload)(implicit ctx: OC, aa: TestAdminAuth): HttpResponse =
      POST(s"v1/review/${ctx.name}", payload, aa.jwtCookie.some)
  }

  case class productReviewApi(productReviewId: Int)(implicit ctx: OC) {
    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(s"v1/review/${ctx.name}/$productReviewId", aa.jwtCookie.some)

    def update(payload: UpdateProductReviewPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(s"v1/review/${ctx.name}/$productReviewId", payload, aa.jwtCookie.some)

    def delete()(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"v1/review/${ctx.name}/$productReviewId", aa.jwtCookie.some)

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

      def get()(implicit aa: TestAdminAuth): HttpResponse =
        GET(path, aa.jwtCookie.some)

      def create(payload: CreateNote)(implicit aa: TestAdminAuth): HttpResponse =
        POST(path, payload, aa.jwtCookie.some)

      case class note(id: Int) {
        val notePath = s"$path/$id"

        def update(payload: UpdateNote)(implicit aa: TestAdminAuth): HttpResponse =
          PATCH(notePath, payload, aa.jwtCookie.some)

        def delete()(implicit aa: TestAdminAuth): HttpResponse =
          DELETE(notePath, aa.jwtCookie.some)
      }
    }
  }

  object storeCreditsApi {
    val storeCreditsPrefix = s"$rootPrefix/store-credits"

    def update(payload: StoreCreditBulkUpdateStateByCsr)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(storeCreditsPrefix, payload, aa.jwtCookie.some)
  }

  case class storeCreditsApi(id: Int) {
    val storeCreditPath = s"${storeCreditsApi.storeCreditsPrefix}/$id"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(storeCreditPath, aa.jwtCookie.some)

    def update(payload: StoreCreditUpdateStateByCsr)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(storeCreditPath, payload, aa.jwtCookie.some)
  }

  object notificationsApi {
    val notificationsPrefix = s"$rootPrefix/notifications"

    def create(payload: CreateNotification)(implicit aa: TestAdminAuth): HttpResponse =
      POST(notificationsPrefix, payload, aa.jwtCookie.some)

    def updateLastSeen(activityId: Int)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$notificationsPrefix/last-seen/$activityId", aa.jwtCookie.some)
  }

  object catalogsApi {
    val catalogsPrefix = s"$rootPrefix/catalogs"

    def create(payload: CreateCatalogPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(catalogsPrefix, payload, aa.jwtCookie.some)
  }

  case class catalogsApi(catalogId: Int) {
    val catalogPath = s"${catalogsApi.catalogsPrefix}/$catalogId"

    def get()(implicit aa: TestAdminAuth): HttpResponse =
      GET(catalogPath, aa.jwtCookie.some)

    def update(payload: UpdateCatalogPayload)(implicit aa: TestAdminAuth): HttpResponse =
      PATCH(catalogPath, payload, aa.jwtCookie.some)

    def addProducts(payload: AddProductsPayload)(implicit aa: TestAdminAuth): HttpResponse =
      POST(s"$catalogPath/products", payload, aa.jwtCookie.some)

    def deleteProduct(productId: Int)(implicit aa: TestAdminAuth): HttpResponse =
      DELETE(s"$catalogPath/products/$productId", aa.jwtCookie.some)
  }

  object captureApi {
    val productPath = s"$rootPrefix/service/capture"

    def capture(payload: CapturePayloads.Capture)(implicit ca: TestAdminAuth): HttpResponse =
      POST(productPath, payload, ca.jwtCookie.some)
  }
}
