/* @flow weak */

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import createAsyncActions from './async-utils';

export const toggleCart = createAction('TOGGLE_CART');
export const hideCart = createAction('HIDE_CART');
export const updateCart = createAction('UPDATE_CART');
export const selectShippingMethod = createAction('CART_SET_SHIPPING_METHOD');
export const selectCreditCard = createAction('CART_SET_CREDIT_CARD');
export const resetCreditCard = createAction('CART_RESET_CREDIT_CARD');
export const resetCart = createAction('RESET_CART');

export type ProductInCart = {
  skuId: number;
  quantity: number;
  imagePath: string;
  referenceNumber: string;
  name: string;
  sku: string;
  price: number;
  quantity: number;
  totalPrice: number;
  state: string;
};

type Totals = {
  subTotal: number;
  taxes: number;
  shipping: number;
  adjustments: number;
  total: number;
}

type FormData = {
  isVisible: boolean;
  totals: Totals;
  skus: Array<ProductInCart>;
};

// reduce SKU list
function collectLineItems(skus) {
  return _.map(skus, (s) => {
    const totalPrice = s.quantity * s.price;
    return {
      ...s,
      totalPrice,
    };
  });
}

// get line items from response
function getLineItems(payload) {
  const skus = _.get(payload, ['lineItems', 'skus'], []);
  const reducedSkus = collectLineItems(skus);
  return reducedSkus;
}

// collect items for submit
function collectItemsToSubmit(items) {
  return _.map(items, (item) => ({
    sku: item.sku,
    quantity: item.quantity,
  }));
}

// collect line items to submit change
function addToLineItems(items, id, quantity) {
  const toCollect = items.concat([{sku: id, quantity}]);
  return collectItemsToSubmit(toCollect);
}

function changeCart(payload) {
  return this.api.post('/v1/my/cart/line-items', payload);
}

const { fetch: submitChange, ...changeCartActions } = createAsyncActions('cartChange', changeCart);

// add line item to cart
export function addLineItem(id, quantity) {
  return (dispatch, getState) => {
    const state = getState();
    const lineItems = _.get(state, ['cart', 'skus'], []);
    const newLineItems = addToLineItems(lineItems, id, quantity);
    return dispatch(submitChange(newLineItems));
  };
}

// remove item from cart
export function deleteLineItem(id) {
  return (dispatch, getState) => {
    const state = getState();
    const lineItems = _.get(state, ['cart', 'skus'], []);
    const newLineItems = _.map(lineItems, (item) => {
      const quantity = item.sku === id ? 0 : item.quantity;
      return {
        sku: item.sku,
        quantity,
      };
    });
    return dispatch(submitChange(newLineItems));
  };
}

function fetchMyCart(): global.Promise {
  return this.api.get(`/v1/my/cart`);
}

const {fetch, ...actions} = createAsyncActions('cart', fetchMyCart);

const initialState: FormData = {
  isVisible: false,
  skus: [],
  quantity: 0,
  totals: {
    subTotal: 0,
    taxes: 0,
    shipping: 0,
    adjustments: 0,
    total: 0,
  },
};

function totalSkuQuantity(cart) {
  return _.reduce(cart.lineItems.skus, (sum, sku) => {
    return sum + sku.quantity;
  }, 0);
}

function updateCartState(state, cart) {
  const data = getLineItems(cart);
  const quantity = totalSkuQuantity(cart);

  return {
    ...state,
    skus: data,
    quantity,
    ...cart,
  };
}

const reducer = createReducer({
  [toggleCart]: state => {
    const currentState = _.get(state, 'isVisible', false);
    return {
      ...state,
      isVisible: !currentState,
    };
  },
  [hideCart]: state => {
    return {
      ...state,
      isVisible: false,
    };
  },
  [changeCartActions.succeeded]: (state, { result }) => {
    const data = getLineItems(result);
    const quantity = totalSkuQuantity(result);
    const totals = _.get(result, ['totals'], {});
    return {
      ...state,
      totals,
      quantity,
      skus: data,
    };
  },
  [actions.succeeded]: updateCartState,
  [updateCart]: updateCartState,
  [selectShippingMethod]: (state, shippingMethod) => {
    return {
      ...state,
      shippingMethod,
    };
  },
  [selectCreditCard]: (state, creditCard) => {
    return {
      ...state,
      creditCard,
    };
  },
  [resetCreditCard]: (state) => {
    return {
      ...state,
      creditCard: null,
    };
  },
  [resetCart]: () => {
    return initialState;
  },
}, initialState);

export {
  fetch,
  reducer as default,
};
