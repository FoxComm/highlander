import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';
import { get, assoc } from 'sprout-data';
import _ from 'lodash';

const _createLineItemAction = (description, ...args) => {
  return createAction('ORDER_LINE_ITEMS_' + description, ...args);
};

export const orderRequest = createAction('ORDER_REQUEST');
export const orderSuccess = createAction('ORDER_SUCCESS');
export const orderFailed = createAction('ORDER_FAILED', (err, source) => [err, source]);
export const orderLineItemsStartEdit = _createLineItemAction('START_EDIT');
export const orderLineItemsCancelEdit = _createLineItemAction('CANCEL_EDIT');
export const orderLineItemsRequest = _createLineItemAction('REQUEST');
export const orderLineItemsRequestSuccess = _createLineItemAction('REQUEST_SUCCESS');
export const orderLineItemsRequestFailed =
  _createLineItemAction('REQUEST_FAILED', (err, source) => [err, source]);
export const orderLineItemsStartDelete = _createLineItemAction('START_DELETE');
export const orderLineItemsCancelDelete = _createLineItemAction('CANCEL_DELETE');

export function fetchOrder(refNum) {
  return dispatch => {
    dispatch(orderRequest(refNum));
    return Api.get(`/orders/${refNum}`)
      .then(order => dispatch(orderSuccess(order)))
      .catch(err => dispatch(orderFailed(err, fetchOrder)));
  };
}

export function updateOrder(id, data) {
  return dispatch => {
    dispatch(orderRequest(id));
    Api.patch(`/orders/${id}`, data)
      .then(order => dispatch(orderSuccess(order)))
      .catch(err => dispatch(orderFailed(id, err, updateOrder)));
  };
}

export function updateLineItemCount(order, sku, quantity, confirmDelete = true) {
  return dispatch => {
    if (quantity == 0 && confirmDelete) {
      dispatch(orderLineItemsStartDelete(sku));
    } else {
      dispatch(orderLineItemsRequest(sku));

      const payload = [{ sku: sku, quantity: quantity }];
      return Api.post(`/orders/${order.referenceNumber}/line-items`, payload)
        .then(order => {
          dispatch(orderLineItemsRequestSuccess(sku));
          dispatch(orderSuccess(order.result));
        })
        .catch(err => dispatch(orderLineItemsRequestFailed(err, updateLineItemCount)));
    }
  };
}

export function deleteLineItem(order, sku) {
  return updateLineItemCount(order, sku, 0, false);
}

function collectLineItems(skus) {
  const uniqueSkus = _.unique(skus, 'sku');
  const quantities = skus.reduce((quantities, elem) => {
    if (quantities[elem.sku]) {
      quantities[elem.sku]++;
    } else {
      quantities[elem.sku] = 1;
    }
    return quantities;
  }, {});
  const itemList = uniqueSkus.map((item, idx) => {
    return assoc(item, 'quantity', quantities[item.sku]);
  });

  return itemList;
}

const initialState = {
  isFetching: false,
  currentOrder: {},
  lineItems: {
    isEditing: false,
    isUpdating: false,
    isDeleting: false,
    skuToUpdate: null,
    skuToDelete: null,
    items: []
  }
};

const reducer = createReducer({
  [orderRequest]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [orderSuccess]: (state, payload) => {
    console.log(payload);
    const skus = _.get(payload, 'lineItems.skus', []);
    const itemList = collectLineItems(skus);
    return {
      ...state,
      isFetching: false,
      currentOrder: haveType(payload, 'order'),
      lineItems: {
        ...state.lineItems,
        items: itemList
      }
    };
  },
  [orderFailed]: (state, [err, source]) => {
    if (source === fetchOrder) {
      console.error(err);

      return {
        ...state,
        isFetching: false
      };
    }

    return state;
  },
  [orderLineItemsStartEdit]: (state) => {
    return assoc(state, ['lineItems', 'isEditing'], true);
  },
  [orderLineItemsCancelEdit]: (state) => {
    const skus = get(state, ['currentOrder', 'lineItems', 'skus'], []);
    return assoc(state,
      ['lineItems', 'isEditing'], false,
      ['lineItems', 'items'], collectLineItems(skus));
  },
  [orderLineItemsRequest]: (state, sku) => {
    return assoc(state,
      ['lineItems', 'isUpdating'], true,
      ['lineItems', 'skuToUpdate'], sku);
  },
  [orderLineItemsRequestSuccess]: (state, sku) => {
    if (sku === state.lineItems.skuToUpdate) {
      return assoc(state,
        ['lineItems', 'isUpdating'], false,
        ['lineItems', 'isDeleting'], false,
        ['lineItems', 'skuToUpdate'], null,
        ['lineItems', 'skuToDelete'], null);
    }

    return state;
  },
  [orderLineItemsRequestFailed]: (state, [err, source]) => {
    console.log(err);

    if (source === updateLineItemCount) {
      return assoc(state,
        ['lineItems', 'isUpdating'], false,
        ['lineItems', 'isDeleting'], false,
        ['lineItems', 'skuToUpdate'], null,
        ['lineItems', 'skuToDelete'], null);
    }

    return state;
  },
  [orderLineItemsStartDelete]: (state, sku) => {
    return assoc(state,
      ['lineItems', 'isDeleting'], true,
      ['lineItems', 'skuToDelete'], sku);
  },
  [orderLineItemsCancelDelete]: (state, sku) => {
    if (state.lineItems.skuToDelete === sku) {
      return assoc(state,
        ['lineItems', 'isDeleting'], false,
        ['lineItems', 'skuToDelete'], null);
    }
  }
}, initialState);

export default reducer;
