/* @flow weak */

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import { createAsyncActions } from '@foxcommerce/wings';
import { updateCart, resetCart, cleanShippingAddress } from 'modules/cart';
import { api as foxApi } from '../lib/api';
import * as tracking from 'lib/analytics';

import type { Address } from 'types/address';
import type { CreditCardType } from 'pages/checkout/types';

export const EditStages = {
  SHIPPING: 0,
  DELIVERY: 1,
  BILLING: 2,
  FINISHED: 3,
  GUEST_AUTH: 4,
};

export type EditStage = number;

export type ShippingAddress = {
  city?: string,
};

export type CheckoutState = {
  creditCard: CreditCardType|null,
  editStage: EditStage,
  shippingAddress: ShippingAddress,
  billingAddress: ShippingAddress,
};

export const selectCreditCard = createAction('CHECKOUT_SET_CREDIT_CARD');
export const setEditStage = createAction('CHECKOUT_SET_EDIT_STAGE');
export const setBillingData = createAction('CHECKOUT_SET_BILLING_DATA', (key, value) => [key, value]);
export const resetBillingData = createAction('CHECKOUT_RESET_BILLING_DATA');
export const loadBillingData = createAction('CHECKOUT_LOAD_BILLING_DATA');
export const setBillingAddress = createAction('CHECKOUT_SET_BILLING_ADDRESS');
export const toggleShippingModal = createAction('TOGGLE_SHIPPING_MODAL');
export const toggleDeliveryModal = createAction('TOGGLE_DELIVERY_MODAL');
export const togglePaymentModal = createAction('TOGGLE_PAYMENT_MODAL');
const markAddressAsDeleted = createAction('CHECKOUT_MARK_ADDRESS_AS_DELETED');
const markAddressAsRestored = createAction(
  'CHECKOUT_MARK_ADDRESS_AS_RESTORED',
  (oldId: number, newAddress: Address) => [oldId, newAddress]
);
const markDefaultAddress = createAction(
  'MARK_ADDRESS_AS_DEFAULT',
  id => id
);
export const cleanDeletedAddresses = createAction('CHECKOUT_CLEAN_DELETED_ADDRESSES');

export const resetCheckout = createAction('CHECKOUT_RESET');
const orderPlaced = createAction('CHECKOUT_ORDER_PLACED');

/* eslint-disable quotes, quote-props */

function _fetchShippingMethods() {
  return foxApi.cart.getShippingMethods();
}

function _fetchCreditCards() {
  return foxApi.creditCards.list();
}

const _resetCreditCard = createAsyncActions(
  'resetCreditCard',
  function() {
    return foxApi.cart.removeCreditCards();
  }
);

export const resetCreditCard = _resetCreditCard.perform;

/* eslint-enable quotes, quote-props */

const shippingMethodsActions = createAsyncActions('shippingMethods', _fetchShippingMethods);
const creditCardsActions = createAsyncActions('creditCards', _fetchCreditCards);
const _fetchAddresses = createAsyncActions('addresses', () => foxApi.addresses.list());

export const fetchShippingMethods = shippingMethodsActions.fetch;
export const fetchCreditCards = creditCardsActions.fetch;
export const fetchAddresses = _fetchAddresses.fetch;

function stripPhoneNumber(phoneNumber: string): string {
  return phoneNumber.replace(/[^\d]/g, '');
}

function addressToPayload(address) {
  const payload = _.pick(address, [
    'name',
    'address1',
    'address2',
    'city',
    'zip',
    'phoneNumber',
    'isDefault',
    'id',
  ]);
  payload.phoneNumber = stripPhoneNumber(payload.phoneNumber);
  payload.regionId = _.get(address, 'region.id', _.get(address, 'state.id', ''));

  return payload;
}

const _removeShippingMethod = createAsyncActions(
  'removeShippingMethod',
  function() {
    const { dispatch } = this;

    return foxApi.cart.removeShippingMethod()
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  }
);

export const removeShippingMethod = _removeShippingMethod.perform;

const _saveShippingAddress = createAsyncActions(
  'saveShippingAddress',
  function(id: number) {
    const { dispatch } = this;

    return foxApi.cart.setShippingAddressById(id)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  }
);

export const saveShippingAddress = _saveShippingAddress.perform;

const _addShippingAddress = createAsyncActions(
  'addShippingAddress',
  function(address) {
    const { dispatch } = this;
    const payload = addressToPayload(address);
    return foxApi.cart.setShippingAddress(payload)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  }
);

export const addShippingAddress = _addShippingAddress.perform;

const _updateShippingAddress = createAsyncActions(
  'updateShippingAddress',
  function(address) {
    const { dispatch } = this;
    const payload = addressToPayload(address);
    return foxApi.cart.updateShippingAddress(payload)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  }
);

export const updateShippingAddress = _updateShippingAddress.perform;

const _setAddressAsDefault = createAsyncActions(
  'setAddressAsDefault',
  function (id) {
    const { dispatch } = this;
    // optimistic update
    dispatch(markDefaultAddress(id));
    return foxApi.addresses.setAsDefault(id)
      .then(() => {
        dispatch(fetchAddresses());
      });
  }
);

export const setAddressAsDefault = _setAddressAsDefault.perform;

const _removeShippingAddress = createAsyncActions(
  'REMOVE_SHIPPING_ADDRESS',
  function() {
    const { getState, dispatch } = this;
    const { shippingAddress, shippingMethod } = getState().cart;
    if (_.isEmpty(shippingAddress)) return Promise.resolve({});

    return foxApi.cart.removeShippingAddress()
      .then(() => {
        dispatch(cleanShippingAddress());
        if (!_.isEmpty(shippingMethod)) {
          dispatch(removeShippingMethod());
        }
      });
  }
);

export const removeShippingAddress = _removeShippingAddress.perform;

const _saveShippingMethod = createAsyncActions(
  'saveShippingMethod',
  function(shippingMethod) {
    const { dispatch, api } = this;
    const methodId = shippingMethod.id;

    return api.cart.chooseShippingMethod(methodId)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  }
);

export const saveShippingMethod = _saveShippingMethod.perform;

const _saveGiftCard = createAsyncActions(
  'saveGiftCard',
  function(code) {
    const { dispatch } = this;
    const payload = { code: code.trim() };

    return foxApi.cart.addGiftCard(payload)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  }
);

export const saveGiftCard = _saveGiftCard.perform;

const _saveCouponCode = createAsyncActions(
  'saveCouponCode',
  function(code) {
    const { dispatch } = this;

    return foxApi.cart.addCoupon(code)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  }
);

export const saveCouponCode = _saveCouponCode.perform;

export function removeGiftCard(code: string): Function {
  return (dispatch) => {
    return foxApi.cart.removeGiftCard(code)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  };
}

export function removeCouponCode() {
  return (dispatch) => {
    return foxApi.cart.removeCoupon()
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  };
}

function setDefaultCard(id: number, isDefault: boolean): Function {
  return () => {
    return foxApi.creditCards.setAsDefault(id, isDefault);
  };
}

function createOrUpdateAddress(payload, id) {
  if (id) {
    return foxApi.addresses.update(id, payload);
  }
  return foxApi.addresses.add(payload);
}

function autoAssignNewDefaultShippingAddress() {
  return (dispatch, getState) => {
    const { addresses } = getState().checkout;

    const newDefault = _.find(addresses, address => !address.isDeleted);
    if (newDefault) {
      dispatch(setAddressAsDefault(newDefault.id));
    } else {
      dispatch(removeShippingAddress());
    }
  };
}

const _deleteAddress = createAsyncActions(
  'deleteAddress',
  function(addressId: number) {
    const { dispatch, getState } = this;
    const address = _.find(getState().checkout.addresses, {id: addressId});
    const wasDefault = address.isDefault;

    return foxApi.addresses.delete(addressId).then(() => {
      dispatch(markAddressAsDeleted(addressId));
      if (wasDefault) {
        dispatch(autoAssignNewDefaultShippingAddress());
      }
    });
  }
);

export const deleteAddress = _deleteAddress.perform;

const _updateAddress = createAsyncActions(
  'updateAddress',
  function(address: Address, id?: number) {
    const payload = addressToPayload(address);
    const { dispatch, getState } = this;
    const { addresses } = getState().checkout;
    const emptyAddressList = _.filter(addresses, addr => !addr.isDeleted).length == 0;

    let result;

    return createOrUpdateAddress(payload, id)
      .then((addressResponse) => {
        result = addressResponse;
        if (payload.isDefault || emptyAddressList) {
          return dispatch(setAddressAsDefault(addressResponse.id));
        }
        return dispatch(fetchAddresses());
      })
      .then(() => result);
  }
);

const _restoreAddress = createAsyncActions(
  'restoreAddress',
  function(addressId) {
    const { dispatch, getState } = this;
    const address = _.find(getState().checkout.addresses, {id: addressId});
    if (address) {
      const payload = addressToPayload(address);
      const promise = foxApi.addresses.add(payload).then((response) => {
        dispatch(markAddressAsRestored(addressId, response));
        return response;
      });
      if (payload.isDefault) {
        return promise.then((response) => {
          return foxApi.addresses.setAsDefault(response.id);
        });
      }
      return promise;
    }
    return Promise.reject(new Error(`There is no address with id ${addressId}`));
  }
);

export const restoreAddress = _restoreAddress.perform;

export const updateAddress = _updateAddress.perform;

function getUpdatedBillingAddress(getState, billingAddressIsSame) {
  return billingAddressIsSame
    ? getState().cart.shippingAddress
    : getState().checkout.billingAddress;
}

const _addCreditCard = createAsyncActions(
  'addCreditCard',
  function(billingAddressIsSame: boolean) {
    const { dispatch, getState } = this;

    const billingData = getState().checkout.billingData;
    const cardData = _.pick(billingData, ['holderName', 'number', 'cvc', 'expMonth', 'expYear', 'isDefault']);
    const billingAddress = getUpdatedBillingAddress(getState, billingAddressIsSame);
    const address = addressToPayload(billingAddress);

    return foxApi.creditCards.create(cardData, address, !billingAddressIsSame).then((newCard) => {
      if (cardData.isDefault) {
        dispatch(setDefaultCard(newCard.id, cardData.isDefault));
      }
      return newCard;
    });
  }
);

export const addCreditCard = _addCreditCard.perform;
export const clearAddCreditCardErrors = _addCreditCard.clearErrors;

export function chooseCreditCard(): Function {
  return (dispatch, getState) => {
    const creditCard = getState().checkout.creditCard;

    return foxApi.cart.addCreditCard(creditCard.id)
      .then((res) => {
        dispatch(updateCart(res.result));
      });
  };
}

const _updateCreditCard = createAsyncActions(
  'updateCreditCard',
  function(id, billingAddressIsSame: boolean) {
    const { getState } = this;

    const creditCard = getState().checkout.billingData;
    const billingAddress = getUpdatedBillingAddress(getState, billingAddressIsSame);
    const address = addressToPayload(billingAddress);
    const updatedCard = assoc(creditCard, 'address', address);

    return foxApi.creditCards.update(id, updatedCard);
  }
);

export const updateCreditCard = _updateCreditCard.perform;
export const clearUpdateCreditCardErrors = _updateCreditCard.clearErrors;

export function deleteCreditCard(id): Function {
  return (dispatch, getState) => {
    if (_.get(getState().checkout, 'creditCard.id') == id) {
      dispatch(resetCreditCard());
    }
    if (_.get(getState().checkout, 'billingData.id') == id) {
      dispatch(resetBillingData());
    }
    return foxApi.creditCards.delete(id)
      .then(() => dispatch(fetchCreditCards()));
  };
}

// Place order from cart.
const _checkout = createAsyncActions(
  'checkout',
  function() {
    const { dispatch, getState } = this;
    const cartState = getState().cart;

    return foxApi.cart.checkout().then((res) => {
      tracking.purchase({
        ...cartState,
        referenceNumber: res.referenceNumber,
      });
      dispatch(orderPlaced(res));
      dispatch(resetCart());
      return res;
    });
  }
);

export const checkout = _checkout.perform;
export const clearCheckoutErrors = _checkout.clearErrors;

function setEmptyCard() {
  return {
    holderName: '',
    number: '',
    cvc: '',
    expMonth: '',
    expYear: '',
  };
}

const initialState: CheckoutState = {
  editStage: EditStages.SHIPPING,
  shippingAddress: {},
  billingAddress: {},
  billingData: {},
  shippingMethods: [],
  creditCards: [],
  addresses: [],
  isAddressLoaded: false,
  creditCard: null,
  shippingModalVisible: false,
  deliveryModalVisible: false,
  paymentModalVisible: false,
};

function sortAddresses(addresses: Array<Address>): Array<Address> {
  return addresses.slice().sort((a, b) => {
    if (!a.isDeleted && !b.isDeleted) return a.id - b.id;
    if (a.isDeleted) return 1;
    if (b.isDeleted) return -1;
    return 0;
  });
}

const reducer = createReducer({
  [setEditStage]: (state, editStage: EditStage) => {
    return {
      ...state,
      editStage,
    };
  },
  [setBillingData]: (state, [key, value]) => {
    return assoc(state,
      ['billingData', key], value
    );
  },
  [setBillingAddress]: (state, address) => {
    return assoc(state,
      'billingAddress', address
    );
  },
  [resetBillingData]: (state) => {
    return {
      ...state,
      billingData: setEmptyCard(),
    };
  },
  [loadBillingData]: (state, billingData) => {
    return {
      ...state,
      billingData,
    };
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
  [_fetchAddresses.succeeded]: (state, list) => {
    const deletedAddresses = _.filter(state.addresses, {isDeleted: true});
    const addresses = [...list, ...deletedAddresses];
    return {
      ...state,
      addresses,
    };
  },
  [cleanDeletedAddresses]: (state) => {
    return {
      ...state,
      addresses: _.filter(state.addresses, address => !address.isDeleted),
    };
  },
  [markDefaultAddress]: (state, addressId) => {
    const newAddresses = _.map(state.addresses, (address) => {
      if (address.id === addressId) {
        return {...address, isDefault: true};
      } else if (address.isDefault) {
        return {...address, isDefault: false};
      }
      return address;
    });
    return {
      ...state,
      addresses: newAddresses,
    };
  },
  [markAddressAsDeleted]: (state, addressId) => {
    const index = _.findIndex(state.addresses, {id: addressId});
    if (index != -1) {
      const newAddresses = assoc(state.addresses,
        [index, 'isDeleted'], true,
        [index, 'isDefault'], false
      );
      return {
        ...state,
        addresses: sortAddresses(newAddresses),
      };
    }
    return state;
  },
  [markAddressAsRestored]: (state, [addressId, addressData]) => {
    const index = _.findIndex(state.addresses, {id: addressId});
    if (index != -1) {
      const newAddresses = assoc(state.addresses, index, addressData);
      return {
        ...state,
        addresses: sortAddresses(newAddresses),
      };
    }
    return state;
  },
  [toggleShippingModal]: (state) => {
    const visibleCurrent = _.get(state, 'shippingModalVisible', false);
    return {
      ...state,
      shippingModalVisible: !visibleCurrent,
    };
  },
  [toggleDeliveryModal]: (state) => {
    const visibleCurrent = _.get(state, 'deliveryModalVisible', false);
    return {
      ...state,
      deliveryModalVisible: !visibleCurrent,
    };
  },
  [togglePaymentModal]: (state) => {
    const visibleCurrent = _.get(state, 'paymentModalVisible', false);
    return {
      ...state,
      paymentModalVisible: !visibleCurrent,
    };
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
  [selectCreditCard]: (state, creditCard) => {
    return {
      ...state,
      creditCard,
    };
  },
  [_resetCreditCard.succeeded]: (state) => {
    return {
      ...state,
      creditCard: null,
    };
  },
}, initialState);

export default reducer;
