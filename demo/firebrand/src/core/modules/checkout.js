/* @flow weak */

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import createAsyncActions from './async-utils';
import { fetchCountry } from 'modules/countries';
import { updateCart } from 'modules/cart';
import { api as foxApi } from '../lib/api';

export const AddressKind = {
  SHIPPING: 0,
  BILLING: 1,
};

export type AddressKindType = number;

export const EditStages = {
  SHIPPING: 0,
  DELIVERY: 1,
  BILLING: 2,
  FINISHED: 3,
};

export type EditStage = number;

export type ShippingAddress = {
  city?: string;
}

export type CheckoutState = {
  editStage: EditStage;
  shippingAddress: ShippingAddress;
  billingAddress: ShippingAddress;
};

export type BillingData = {
  holderName?: string;
  number?: string|number;
  brand?: string;
  expMonth?: string|number;
  expYear?: string|number;
  lastFour?: string|number;
}

export const setEditStage = createAction('CHECKOUT_SET_EDIT_STAGE');
export const setBillingData = createAction('CHECKOUT_SET_BILLING_DATA', (key, value) => [key, value]);

export const setAddressData = createAction('CHECKOUT_SET_ADDRESS_DATA', (kind, key, value) => [kind, key, value]);
export const extendAddressData = createAction('CHECKOUT_EXTEND_ADDRESS_DATA', (kind, props) => [kind, props]);
export const resetCheckout = createAction('CHECKOUT_RESET');
const orderPlaced = createAction('CHECKOUT_ORDER_PLACED');

/* eslint-disable quotes, quote-props */

function _fetchShippingMethods() {
  return this.api.get('/v1/my/cart/shipping-methods');
}

function _fetchCreditCards() {
  return foxApi.creditCards.list();
}

/* eslint-enable quotes, quote-props */

const shippingMethodsActions = createAsyncActions('shippingMethods', _fetchShippingMethods);
const creditCardsActions = createAsyncActions('creditCards', _fetchCreditCards);

export const fetchShippingMethods = shippingMethodsActions.fetch;
export const fetchCreditCards = creditCardsActions.fetch;
export const toggleSeparateBillingAddress = createAction('CHECKOUT_TOGGLE_BILLING_ADDRESS');

export function initAddressData(kind: AddressKindType): Function {
  return (dispatch, getState) => {
    const state = getState();

    const shippingAddress = getState().cart.shippingAddress;

    const countries = state.countries.list;

    const usaCountry = _.find(countries, { alpha3: 'USA' });
    const countryDetails = state.countries.details[usaCountry && usaCountry.id] || { regions: [] };

    let uiAddressData = {
      country: usaCountry,
      state: countryDetails.regions[0],
    };

    if (kind == AddressKind.SHIPPING && shippingAddress && shippingAddress.region) {
      dispatch(fetchCountry(shippingAddress.region.countryId)).then(() => {
        const countryInfo = getState().countries.details[shippingAddress.region.countryId];

        uiAddressData = _.pick(shippingAddress, ['name', 'address1', 'address2', 'city', 'zip', 'phoneNumber']);
        uiAddressData.country = countryInfo;
        uiAddressData.state = _.find(countryInfo.regions, { id: shippingAddress.region.id });

        dispatch(extendAddressData(kind, uiAddressData));
      });
    } else {
      dispatch(extendAddressData(kind, uiAddressData));
    }
  };
}

function addressToPayload(address, countries = []) {
  const payload = _.pick(address, ['name', 'address1', 'address2', 'city', 'zip', 'phoneNumber']);
  payload.phoneNumber = String(payload.phoneNumber);
  payload.regionId = _.get(address, 'region.id', _.get(address, 'state.id', ''));

  if (!_.isEmpty(countries)) {
    const countryId = _.get(address, 'region.countryId', _.get(address, 'state.countryId', ''));
    payload.state = _.get(address, 'region.name', _.get(address, 'state.name', ''));
    payload.country = _.get(countries.filter(country => country.id === countryId), '[0].name', '');
  }

  return payload;
}

export function saveShippingAddress(): Function {
  return (dispatch, getState, api) => {
    const shippingAddress = getState().checkout.shippingAddress;
    const payload = addressToPayload(shippingAddress);

    return api.post('/v1/my/cart/shipping-address', payload)
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

export function saveShippingMethod(): Function {
  return (dispatch, getState, api) => {
    const payload = {
      shippingMethodId: getState().cart.shippingMethod.id,
    };

    return api.patch('/v1/my/cart/shipping-method', payload)
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

export function saveGiftCard(code: string): Function {
  return (dispatch, getState, api) => {
    const payload = { code: code.trim() };

    return api.post('/v1/my/cart/payment-methods/gift-cards', payload)
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

export function saveCouponCode(code: string): Function {
  return (dispatch, getState, api) => {
    return api.post(`/v1/my/cart/coupon/${code.trim()}`, {})
      .then(res => {
        dispatch(updateCart(res));
      });
  };
}

export function addCreditCard(): Function {
  return (dispatch, getState) => {
    const creditCard = getState().cart.creditCard;

    if (creditCard && creditCard.id) {
      return foxApi.cart.addCreditCard(creditCard.id);
    }

    let billingAddress;

    const cardData = _.pick(getState().checkout.billingData, ['holderName', 'number', 'cvc', 'expMonth', 'expYear']);

    if (getState().checkout.billingAddressIsSame) {
      billingAddress = getState().cart.shippingAddress;
    } else {
      billingAddress = getState().checkout.billingAddress;
    }

    const address = addressToPayload(billingAddress, getState().countries.list);

    return foxApi.creditCards.create(cardData, address, !getState().checkout.billingAddressIsSame)
      .then(creditCardRes => {
        return foxApi.cart.addCreditCard(creditCardRes.id);
      })
      .then(res => {
        dispatch(updateCart(res.result));
      });
  };
}

// Place order from cart.
export function checkout(): Function {
  return (dispatch, getState, api) => {
    return api.post('/v1/my/cart/checkout').then(res => {
      dispatch(orderPlaced(res));
      return dispatch(updateCart(res));
    });
  };
}

const initialState: CheckoutState = {
  editStage: EditStages.SHIPPING,
  shippingAddress: {},
  billingAddress: {},
  billingData: {},
  billingAddressIsSame: true,
  shippingMethods: [],
  creditCards: [],
};

const reducer = createReducer({
  [setEditStage]: (state, editStage: EditStage) => {
    return {
      ...state,
      editStage,
    };
  },
  [setAddressData]: (state, [kind, key, value]) => {
    const ns = kind == AddressKind.SHIPPING ? 'shippingAddress' : 'billingAddress';
    return assoc(state,
      [ns, key], value
    );
  },
  [extendAddressData]: (state, [kind, props]) => {
    const ns = kind == AddressKind.SHIPPING ? 'shippingAddress' : 'billingAddress';
    return assoc(state,
      [ns], props
    );
  },
  [setBillingData]: (state, [key, value]) => {
    return assoc(state,
      ['billingData', key], value
    );
  },
  [shippingMethodsActions.succeeded]: (state, list) => {
    return {
      ...state,
      shippingMethods: list,
    };
  },
  [creditCardsActions.succeeded]: (state, list) => {
    return {
      ...state,
      creditCards: list,
    };
  },
  [toggleSeparateBillingAddress]: state => {
    return assoc(state,
      ['billingAddressIsSame'], !state.billingAddressIsSame
    );
  },
  [resetCheckout]: () => {
    return initialState;
  },
  [orderPlaced]: (state, cart) => {
    return {
      ...state,
      orderPlaced: cart.referenceNumber,
    };
  },
}, initialState);

export default reducer;
