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
  customerNotes: customerId => `/v1/notes/customer/${customerId}`,
  customerNote: (customerId, noteId) => `/v1/notes/customer/${customerId}/${noteId}`,
  customerGroups: '/v1/groups',
  customerGroup: groupId => `/v1/groups/${groupId}`,
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

class CustomerNotes {
  constructor(api) {
    this.api = api;
  }
  list(customerId) {
    return this.api.get(endpoints.customerNotes(customerId));
  }
  create(customerId, createNotePayload) {
    return this.api.post(endpoints.customerNotes(customerId), createNotePayload);
  }
  update(customerId, noteId, updateNotePayload) {
    return this.api.patch(endpoints.customerNote(customerId, noteId), updateNotePayload);
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
    this.customerNotes = new CustomerNotes(this);
    this.customerGroups = new CustomerGroups(this);
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
