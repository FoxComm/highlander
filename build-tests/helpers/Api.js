import FoxCommApi from '@foxcomm/api-js';
import superagent from 'superagent';
import cookie from 'cookie';

const API_BASE_URL = process.env.API_URL;
const STRIPE_KEY = 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk';

const endpoints = {
  customers: '/v1/customers',
  customer: customerId => `/v1/customers/${customerId}`,
  customerAddresses: customerId => `/v1/customers/${customerId}/addresses`,
  customerAddress: (customerId, addressId) => `/v1/customers/${customerId}/addresses/${addressId}`,
  customerCreditCards: customerId => `/v1/customers/${customerId}/payment-methods/credit-cards`,
  customerStoreCredit: customerId => `/v1/customers/${customerId}/payment-methods/store-credit`,
  customerGroups: '/v1/groups',
  customerGroup: groupId => `/v1/groups/${groupId}`,
  skus: context => `/v1/skus/${context}`,
  sku: (context, skuCode) => `/v1/skus/${context}/${skuCode}`,
  skuInventory: skuCode => `/v1/inventory/summary/${skuCode}`,
  products: context => `/v1/products/${context}`,
  product: (context, productId) => `/v1/products/${context}/${productId}`,
  productAlbums: (context, productId) => `/v1/products/${context}/${productId}/albums`,
  productAlbumPosition: (context, productId) => `/v1/products/${context}/${productId}/albums/position`,
  giftCards: '/v1/gift-cards',
  giftCard: giftCardCode => `/v1/gift-cards/${giftCardCode}`,
  promotions: context => `/v1/promotions/${context}`,
  promotion: (context, promotionId) => `/v1/promotions/${context}/${promotionId}`,
  coupons: context => `/v1/coupons/${context}`,
  coupon: (context, couponIdOrCode) => `/v1/coupons/${context}/${couponIdOrCode}`,
  couponCodes: couponId => `/v1/coupons/codes/${couponId}`,
  couponCodesGenerate: couponId => `/v1/coupons/codes/generate/${couponId}`,
  albums: context => `/v1/albums/${context}`,
  album: (context, albumId) => `/v1/albums/${context}/${albumId}`,
  albumImages: (context, albumId) => `/v1/albums/${context}/${albumId}/images`,
  notes: (objectType, objectId) => `/v1/notes/${objectType}/${objectId}`,
  note: (objectType, objectId, noteId) => `/v1/notes/${objectType}/${objectId}/${noteId}`,
  storeAdmins: '/v1/store-admins',
  storeAdmin: storeAdminId => `/v1/store-admins/${storeAdminId}`,
  esStoreAdmins: 'search/admin/store_admins_search_view/_search',
  customerCartPaymentStoreCredit: '/v1/my/cart/payment-methods/store-credit',
  cart: referenceNumber => `/v1/carts/${referenceNumber}`,
  cartLineItems: referenceNumber => `/v1/carts/${referenceNumber}/line-items`,
  cartWatchers: referenceNumber => `/v1/carts/${referenceNumber}/watchers`,
  cartWatcher: (referenceNumber, watcherId) => `/v1/carts/${referenceNumber}/watchers/${watcherId}`,
  shippingMethods: referenceNumber => `/v1/shipping-methods/${referenceNumber}`,
  inventory: skuCode => `/v1/inventory/summary/${skuCode}`,
  inventoryIncrement: stockItemId => `/v1/inventory/stock-items/${stockItemId}/increment`,
  inventoryDecrement: stockItemId => `/v1/inventory/stock-items/${stockItemId}/decrement`,
  inventoryShipments: referenceNumber => `/v1/inventory/shipments/${referenceNumber}`,
  order: referenceNumber => `/v1/orders/${referenceNumber}`,
  orderIncreaseRemorsePeriod: referenceNumber => `/v1/orders/${referenceNumber}/increase-remorse-period`,
  orderWatchers: referenceNumber => `/v1/orders/${referenceNumber}/watchers`,
  orderWatcher: (referenceNumber, watcherId) => `/v1/orders/${referenceNumber}/watchers/${watcherId}`,
  // dev
  creditCardToken: '/v1/credit-card-token',
};

class Customers {
  constructor(api) {
    this.api = api;
  }
  list() {
    return this.api.get(endpoints.customers);
  }
  one(customerId) {
    return this.api.get(endpoints.customer(customerId));
  }
  create(customer) {
    return this.api.post(endpoints.customers, customer);
  }
  update(customerId, customer) {
    return this.api.patch(endpoints.customer(customerId), customer);
  }
}

class CustomerAddresses {
  constructor(api) {
    this.api = api;
  }
  list(customerId) {
    return this.api.get(endpoints.customerAddresses(customerId));
  }
  one(customerId, addressId) {
    return this.api.get(endpoints.customerAddress(customerId, addressId));
  }
  add(customerId, address) {
    return this.api.post(endpoints.customerAddresses(customerId), address);
  }
  update(customerId, addressId, address) {
    return this.api.patch(endpoints.customerAddress(customerId, addressId), address);
  }
  delete(customerId, addressId) {
    return this.api.delete(endpoints.customerAddress(customerId, addressId));
  }
}

class CustomerCreditCards {
  constructor(api) {
    this.api = api;
  }
  list(customerId) {
    return this.api.get(endpoints.customerCreditCards(customerId));
  }
  add(customerId, createCreditCardFromTokenPayload) {
    return this.api.post(endpoints.customerCreditCards(customerId), createCreditCardFromTokenPayload);
  }
}

class CustomerStoreCredit {
  constructor(api) {
    this.api = api;
  }
  create(customerId, createManualStoreCreditPayload) {
    return this.api.post(endpoints.customerStoreCredit(customerId), createManualStoreCreditPayload);
  }
}

class CustomerGroups {
  constructor(api) {
    this.api = api;
  }
  list() {
    return this.api.get(endpoints.customerGroups);
  }
  create(customerDynamicGroupPayload) {
    return this.api.post(endpoints.customerGroups, customerDynamicGroupPayload);
  }
  one(groupId) {
    return this.api.get(endpoints.customerGroup(groupId));
  }
  update(groupId, customerDynamicGroupPayload) {
    return this.api.patch(endpoints.customerGroup(groupId), customerDynamicGroupPayload);
  }
}

class Skus {
  constructor(api) {
    this.api = api;
  }
  create(context, skuPayload) {
    return this.api.post(endpoints.skus(context), skuPayload);
  }
  one(context, skuCode) {
    return this.api.get(endpoints.sku(context, skuCode));
  }
  update(context, skuCode, skuPayload) {
    return this.api.patch(endpoints.sku(context, skuCode), skuPayload);
  }
  archive(context, skuCode) {
    return this.api.delete(endpoints.sku(context, skuCode));
  }
  inventory(skuCode) {
    return this.api.get(endpoints.skuInventory(skuCode));
  }
}

class Products {
  constructor(api) {
    this.api = api;
  }
  create(context, productPayload) {
    return this.api.post(endpoints.products(context), productPayload);
  }
  one(context, productId) {
    return this.api.get(endpoints.product(context, productId));
  }
  update(context, productId, productPayload) {
    return this.api.patch(endpoints.product(context, productId), productPayload);
  }
  archive(context, productId) {
    return this.api.delete(endpoints.product(context, productId));
  }
}

class ProductAlbums {
  constructor(api) {
    this.api = api;
  }
  list(context, productId) {
    return this.api.get(endpoints.productAlbums(context, productId));
  }
  create(context, productId, albumPayload) {
    return this.api.post(endpoints.productAlbums(context, productId), albumPayload);
  }
  updatePosition(context, productId, updateAlbumPositionPayload) {
    return this.api.post(endpoints.productAlbumPosition(context, productId), updateAlbumPositionPayload);
  }
}

class GiftCards {
  constructor(api) {
    this.api = api;
  }
  list() {
    return this.api.get(endpoints.giftCards);
  }
  create(giftCardPayload) {
    return this.api.post(endpoints.giftCards, giftCardPayload);
  }
  one(giftCardCode) {
    return this.api.get(endpoints.giftCard(giftCardCode));
  }
  update(giftCardCode, giftCardPayload) {
    return this.api.patch(endpoints.giftCard(giftCardCode), giftCardPayload);
  }
}

class Promotions {
  constructor(api) {
    this.api = api;
  }
  create(context, promotionPayload) {
    return this.api.post(endpoints.promotions(context), promotionPayload);
  }
  one(context, promotionId) {
    return this.api.get(endpoints.promotion(context, promotionId));
  }
  update(context, promotionId, promotionPayload) {
    return this.api.patch(endpoints.promotion(context, promotionId), promotionPayload);
  }
}

class Coupons {
  constructor(api) {
    this.api = api;
  }
  create(context, couponPayload) {
    return this.api.post(endpoints.coupons(context), couponPayload);
  }
  one(context, couponIdOrCode) {
    return this.api.get(endpoints.coupon(context, couponIdOrCode));
  }
  update(context, couponId, couponPayload) {
    return this.api.patch(endpoints.coupon(context, couponId), couponPayload);
  }
}

class CouponCodes {
  constructor(api) {
    this.api = api;
  }
  list(couponId) {
    return this.api.get(endpoints.couponCodes(couponId));
  }
  generate(couponId, generateCouponCodesPayload) {
    return this.api.post(endpoints.couponCodesGenerate(couponId), generateCouponCodesPayload);
  }
}

class Albums {
  constructor(api) {
    this.api = api;
  }
  create(context, albumPayload) {
    return this.api.post(endpoints.albums(context), albumPayload);
  }
  one(context, albumId) {
    return this.api.get(endpoints.album(context, albumId));
  }
  update(context, albumId, albumPayload) {
    return this.api.patch(endpoints.album(context, albumId), albumPayload);
  }
  archive(context, albumId) {
    return this.api.delete(endpoints.album(context, albumId));
  }
  uploadImages(context, albumId, images) {
    return images
      .reduce((req, file) => req.attach('upload-file', file), this.api.agent
        .post(`${API_BASE_URL}/api/${endpoints.albumImages(context, albumId)}`))
      .withCredentials()
      .then(res => res.body);
  }
}

class Notes {
  constructor(api) {
    this.api = api;
  }
  list(objectType, objectId) {
    return this.api.get(endpoints.notes(objectType, objectId));
  }
  create(objectType, objectId, createNotePayload) {
    return this.api.post(endpoints.notes(objectType, objectId), createNotePayload);
  }
  update(objectType, objectId, noteId, updateNotePayload) {
    return this.api.patch(endpoints.note(objectType, objectId, noteId), updateNotePayload);
  }
}

class Dev {
  constructor(api) {
    this.api = api;
  }
  creditCardToken(creditCardDetails) {
    return this.api.post(endpoints.creditCardToken, creditCardDetails);
  }
}

class StoreAdmins {
  constructor(api) {
    this.api = api;
  }
  list(maxCount = 50) {
    const url = `${API_BASE_URL}/api/${endpoints.esStoreAdmins}`;
    const request = this.api.agent.post(url).withCredentials();
    const cookies = cookie.parse(request.cookies);
    return request
      .query({ size: maxCount })
      .set('JWT', cookies.JWT)
      .send({ query: { bool: {} }, sort: [{ createdAt: { order: 'desc' } }] })
      .then(res => res.body.result);
  }
  create(storeAdminPayload) {
    return this.api.post(endpoints.storeAdmins, storeAdminPayload);
  }
  one(storeAdminId) {
    return this.api.get(endpoints.storeAdmin(storeAdminId));
  }
  update(storeAdminId, storeAdminPayload) {
    return this.api.patch(endpoints.storeAdmin(storeAdminId), storeAdminPayload);
  }
}

class Carts {
  constructor(api) {
    this.api = api;
  }
  get(referenceNumber) {
    return this.api.get(endpoints.cart(referenceNumber));
  }
  getShippingMethods(referenceNumber) {
    return this.api.get(endpoints.shippingMethods(referenceNumber));
  }
  setLineItems(referenceNumber, lineItemsPayload) {
    return this.api.post(endpoints.cartLineItems(referenceNumber), lineItemsPayload);
  }
  updateLineItems(referenceNumber, lineItemsPayload) {
    return this.api.patch(endpoints.cartLineItems(referenceNumber), lineItemsPayload);
  }
  getWatchers(referenceNumber) {
    return this.api.get(endpoints.cartWatchers(referenceNumber));
  }
  addWatchers(referenceNumber, watchersPayload) {
    return this.api.post(endpoints.cartWatchers(referenceNumber), watchersPayload);
  }
  removeWatcher(referenceNumber, watcherId) {
    return this.api.delete(endpoints.cartWatcher(referenceNumber, watcherId));
  }
}

class Inventories {
  constructor(api) {
    this.api = api;
  }
  get(skuCode) {
    return this.api.get(endpoints.inventory(skuCode));
  }
  getShipments(referenceNumber) {
    return this.api.get(endpoints.inventoryShipments(referenceNumber));
  }
  increment(stockItemId, incrementPayload) {
    return this.api.patch(endpoints.inventoryIncrement(stockItemId), incrementPayload);
  }
  decrement(stockItemId, decrementPayload) {
    return this.api.patch(endpoints.inventoryDecrement(stockItemId), decrementPayload);
  }
}

class Orders {
  constructor(api) {
    this.api = api;
  }
  one(referenceNumber) {
    return this.api.get(endpoints.order(referenceNumber));
  }
  update(referenceNumber, orderUpdatePayload) {
    return this.api.patch(endpoints.order(referenceNumber), orderUpdatePayload);
  }
  increaseRemorsePeriod(referenceNumber) {
    return this.api.post(endpoints.orderIncreaseRemorsePeriod(referenceNumber));
  }
  getWatchers(referenceNumber) {
    return this.api.get(endpoints.orderWatchers(referenceNumber));
  }
  addWatchers(referenceNumber, watchersPayload) {
    return this.api.post(endpoints.orderWatchers(referenceNumber), watchersPayload);
  }
  removeWatcher(referenceNumber, watcherId) {
    return this.api.delete(endpoints.orderWatcher(referenceNumber, watcherId));
  }
}

export default class Api extends FoxCommApi {
  constructor(...args) {
    super(...args);
    this.customers = new Customers(this);
    this.customerAddresses = new CustomerAddresses(this);
    this.customerCreditCards = new CustomerCreditCards(this);
    this.customerStoreCredit = new CustomerStoreCredit(this);
    this.customerGroups = new CustomerGroups(this);
    this.skus = new Skus(this);
    this.products = new Products(this);
    this.productAlbums = new ProductAlbums(this);
    this.giftCards = new GiftCards(this);
    this.promotions = new Promotions(this);
    this.coupons = new Coupons(this);
    this.couponCodes = new CouponCodes(this);
    this.albums = new Albums(this);
    this.notes = new Notes(this);
    this.dev = new Dev(this);
    this.storeAdmins = new StoreAdmins(this);
    this.carts = new Carts(this);
    this.inventories = new Inventories(this);
    this.orders = new Orders(this);
    this.monkeypatch();
  }
  monkeypatch() {
    this.cart.addStoreCredit = amount =>
      this.post(endpoints.customerCartPaymentStoreCredit, { amount });
    this.cart.removeStoreCredit = () =>
      this.delete(endpoints.customerCartPaymentStoreCredit);
  }
  static withoutCookies() {
    return new Api({
      api_url: `${API_BASE_URL}/api`,
      stripe_key: STRIPE_KEY,
    });
  }
  static withCookies() {
    return new Api({
      api_url: `${API_BASE_URL}/api`,
      stripe_key: STRIPE_KEY,
      agent: superagent.agent(),
    });
  }
}
