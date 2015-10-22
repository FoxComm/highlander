
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from './state-helpers';

export const orderRequest = createAction('ORDER_REQUEST');
export const orderSuccess = createAction('ORDER_SUCCESS');
export const orderFailed = createAction('ORDER_FAILED', (err, source) => [err, source]);
export const orderLineItemsStartEdit = createAction('ORDER_LINE_ITEMS_START_EDIT');
export const orderLineItemsCancelEdit = createAction('ORDER_LINE_ITEMS_CANCEL_EDIT');
export const orderLineItemsRequest = createAction('ORDER_LINE_ITEMS_REQUEST');
export const orderLineItemsRequestSuccess = createAction('ORDER_LINE_ITEMS_REQUEST_SUCCESS');
export const orderLineItemsRequestFailed =
  createAction('ORDER_LINE_ITEMS_REQUEST_FAILED', (err, source) => [err, source]);
export const orderLineItemsStartDelete = createAction('ORDER_LINE_ITEMS_START_DELETE');
export const orderLineItemsCancelDelete = createAction('ORDER_LINE_ITEMS_CANCEL_DELETE');

export function fetchOrder(refNum) {
  return dispatch => {
    dispatch(orderRequest(refNum));
    return Api.get(`/orders/${refNum}`)
      .then(order => dispatch(orderSuccess(order)))
      .catch(err => dispatch(orderFailed(err, fetchOrder)));
  };
}

function shouldFetchOrder(refNum, state) {
  const order = state.orders.details;
  if (!order) {
    return true;
  } else if (order.isFetching) {
    return false;
  }
  return order.didInvalidate;
}

export function fetchOrderIfNeeded(refNum) {
  return (dispatch, getState) => {
    if (shouldFetchOrder(refNum, getState())) {
      return dispatch(fetchOrder(refNum));
    }
  };
}

export function updateLineItemCount(order, sku, quantity, confirmDelete = true) {
  return dispatch => {
    if (quantity == 0 && confirmDelete) {
      dispatch(orderLineItemsStartDelete(sku));
    } else {
      dispatch(orderLineItemsRequest(sku));

      let payload = [{ sku: sku, quantity: quantity }];
      return Api.post(`/orders/${order.referenceNumber}/line-items`, payload)
        .then(order => {
          dispatch(orderLineItemsRequestSuccess(sku));
          dispatch(orderSuccess(order));
        })
        .catch(err => dispatch(orderLineItemsRequestFailed(err, updateLineItemCount)));
    }
  };
}

export function deleteLineItem(order, sku) {
  return updateLineItemCount(order, sku, 0, false);
}

const initialState = {
  isFetching: false,
  didInvalidate: true,
  currentOrder: {},
  lineItems: {
    isEditing: false,
    isUpdating: false,
    isDeleting: false,
    skuToUpdate: '',
    skuToDelete: '',
    items: []
  }
};

const reducer = createReducer({
  [orderRequest]: (state) => {
    return {
      ...state,
      isFetching: true,
      didInvalidate: false
    };
  },
  [orderSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      didInvalidate: false,
      currentOrder: haveType(payload, 'order'),
      lineItems: {
        ...state.lineItems,
        items: payload.lineItems.skus
      }
    };
  },
  [orderFailed]: (state, [err, source]) => {
    console.error(err);

    if (source === fetchOrder) {
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    }

    return state;
  },
  [orderLineItemsStartEdit]: (state) => {
    return {
      ...state,
      lineItems: {
        ...state.lineItems,
        isEditing: true
      }
    };
  },
  [orderLineItemsCancelEdit]: (state) => {
    return {
      ...state,
      lineItems: {
        ...state.lineItems,
        isEditing: false,
        items: state.currentOrder.lineItems.skus
      }
    };
  },
  [orderLineItemsRequest]: (state, sku) => {
    return {
      ...state,
      lineItems: {
        ...state.lineItems,
        isUpdating: true,
        skuToUpdate: sku
      }
    };
  },
  [orderLineItemsRequestSuccess]: (state, sku) => {
    if (sku === state.lineItems.skuToUpdate) {
      return {
        ...state,
        lineItems: {
          ...state.lineItems,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: '',
          skuToDelete: ''
        }
      };
    }

    return state;
  },
  [orderLineItemsRequestFailed]: (state, [err, source]) => {
    console.log(err);

    if (source === updateLineItemCount) {
      return {
        ...state,
        lineItems: {
          ...state.lineItems,
          isUpdating: false,
          isDeleting: false,
          skuToUpdate: '',
          skuToDelete: ''
        }
      };
    }

    return state;
  },
  [orderLineItemsStartDelete]: (state, sku) => {
    return {
      ...state,
      lineItems: {
        ...state.lineItems,
        isDeleting: true,
        skuToDelete: sku
      }
    };
  },
  [orderLineItemsCancelDelete]: (state, sku) => {
    if (state.lineItems.skuToDelete === sku) {
      return {
        ...state,
        lineItems: {
          ...state.lineItems,
          isDeleting: false,
          skuToDelete: ''
        }
      };
    }
  },
}, initialState);

export default reducer;
