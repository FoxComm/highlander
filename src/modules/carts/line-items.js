import _ from 'lodash';
import Api from 'lib/api';
import { createAction, createReducer } from 'redux-act';
import { get, assoc } from 'sprout-data';
import { cartSuccess } from './details.js';

const _createLineItemAction = (description, ...args) => {
  return createAction('CART_LINE_ITEMS_' + description, ...args);
};

export const orderLineItemsFetchSuccess = _createLineItemAction('FETCH_SUCCESS');
export const orderLineItemsStartEdit = _createLineItemAction('START_EDIT');
export const orderLineItemsCancelEdit = _createLineItemAction('CANCEL_EDIT');
export const orderLineItemsRequest = _createLineItemAction('REQUEST');
export const orderLineItemsRequestSuccess = _createLineItemAction('REQUEST_SUCCESS');
export const orderLineItemsRequestFailed =
  _createLineItemAction('REQUEST_FAILED', (err, source) => [err, source]);
export const orderLineItemsStartDelete = _createLineItemAction('START_DELETE');
export const orderLineItemsCancelDelete = _createLineItemAction('CANCEL_DELETE');

export function updateLineItemCount(order, sku, quantity, confirmDelete = true) {
  return dispatch => {
    if (quantity == 0 && confirmDelete) {
      dispatch(orderLineItemsStartDelete(sku));
    } else {
      dispatch(orderLineItemsRequest(sku));

      const payload = [{ sku: sku, quantity: quantity }];
      return Api.post(`/orders/${order.referenceNumber}/line-items`, payload)
        .then(
          order => {
            // dispatch(cartSuccess(order));
            dispatch(orderLineItemsRequestSuccess(sku));
            dispatch(orderLineItemsFetchSuccess(order));
          },
          err => dispatch(orderLineItemsRequestFailed(err, updateLineItemCount))
        );
    }
  };
}

export function deleteLineItem(order, sku) {
  return updateLineItemCount(order, sku, 0, false);
}

export function collectLineItems(skus) {
  return _.map(skus, (l) => { 
    l.totalPrice = l.quantity * l.price;
    return l;
  });
}

const initialState = {
  currentSkus: [],
  isEditing: false,
  isUpdating: false,
  isDeleting: false,
  skuToUpdate: null,
  skuToDelete: null,
  items: [],
};

const reducer = createReducer({
  [orderLineItemsFetchSuccess]: (state, payload) => {
    const order = _.get(payload, 'result', payload);
    const skus = _.get(order, 'lineItems.skus', []);
    const itemList = collectLineItems(skus);

    return assoc(state,
      ['currentSkus'], skus,
      ['isFetching'], false,
      ['items'], itemList
    );
  },
  [orderLineItemsStartEdit]: (state) => {
    return assoc(state, ['isEditing'], true);
  },
  [orderLineItemsCancelEdit]: (state) => {
      //TODO : How do i access
    const skus = get(state, ['currentSkus'], []);
    return assoc(state,
      ['isEditing'], false,
      ['items'], collectLineItems(skus)
    );
  },
  [orderLineItemsRequest]: (state, sku) => {
    return assoc(state,
      ['isUpdating'], true,
      ['skuToUpdate'], sku
    );
  },
  [orderLineItemsRequestSuccess]: (state, sku) => {
    if (sku === state.skuToUpdate) {
      return assoc(state,
        ['isUpdating'], false,
        ['isDeleting'], false,
        ['skuToUpdate'], null,
        ['skuToDelete'], null
      );
    }

    return state;
  },
  [orderLineItemsRequestFailed]: (state, [err, source]) => {
    console.error(err);

    if (source === updateLineItemCount) {
      return assoc(state,
        ['isUpdating'], false,
        ['isDeleting'], false,
        ['skuToUpdate'], null,
        ['skuToDelete'], null
      );
    }

    return state;
  },
  [orderLineItemsStartDelete]: (state, sku) => {
    return assoc(state,
      ['isDeleting'], true,
      ['skuToDelete'], sku
    );
  },
  [orderLineItemsCancelDelete]: (state, sku) => {
    if (state.skuToDelete === sku) {
      return assoc(state,
        ['isDeleting'], false,
        ['skuToDelete'], null
      );
    }
  }
}, initialState);

export default reducer;
