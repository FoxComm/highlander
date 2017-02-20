import FoxCommApi from '@foxcomm/api-js';
import superagent from 'superagent';

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
  albums: context => `/v1/albums/${context}`,
  album: (context, albumId) => `/v1/albums/${context}/${albumId}`,
  albumImages: (context, albumId) => `/v1/albums/${context}/${albumId}/images`,
  notes: (objectType, objectId) => `/v1/notes/${objectType}/${objectId}`,
  note: (objectType, objectId, noteId) => `/v1/notes/${objectType}/${objectId}/${noteId}`,
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
    return images.reduce((req, file) => req.attach('upload-file', file),
        this.api.agent.post(`${API_BASE_URL}/api/${endpoints.albumImages(context, albumId)}`))
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
    this.albums = new Albums(this);
    this.notes = new Notes(this);
    this.dev = new Dev(this);
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
