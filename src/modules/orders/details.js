
import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';
import { get, assoc } from 'sprout-data';
import OrderParagon from '../../paragons/order';

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
      .then(
        order => dispatch(orderSuccess(order)),
        err => dispatch(orderFailed(err, fetchOrder))
      );
  };
}

export function updateOrder(id, data) {
  return dispatch => {
    dispatch(orderRequest(id));
    Api.patch(`/orders/${id}`, data)
      .then(
        order => dispatch(orderSuccess(order)),
        err => dispatch(orderFailed(id, err, updateOrder))
      );
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
        .then(
          order => {
            dispatch(orderLineItemsRequestSuccess(sku));
            dispatch(orderSuccess(order.result));
          },
          err => dispatch(orderLineItemsRequestFailed(err, updateLineItemCount))
        );
    }
  };
}

export function deleteLineItem(order, sku) {
  return updateLineItemCount(order, sku, 0, false);
}

export function increaseRemorsePeriod(refNum) {
  return dispatch => {
    dispatch(orderRequest(refNum));
    return Api.post(`/orders/${refNum}/increase-remorse-period`)
      .then(
        order => dispatch(orderSuccess(order)),
        err => dispatch(orderFailed(err, fetchOrder))
      );
  };
}

function collectLineItems(skus) {
  let uniqueSkus = {};
  const items = _.transform(skus, (result, lineItem) => {
    const sku = lineItem.sku;
    if (_.isNumber(uniqueSkus[sku])) {
      const qty = result[uniqueSkus[sku]].quantity += 1;
      result[uniqueSkus[sku]].totalPrice = lineItem.price * qty;
    } else {
      uniqueSkus[sku] = result.length;
      result.push({ ...lineItem, quantity: 1 });
    }
  });
  return items;
}

function parseMessages(messages, state) {
  return _.reduce(messages, (results, message) => {
    if (message.indexOf('items') != -1) {
      return { ...results, itemsStatus: state };
    } else if (message.indexOf('empty cart') != -1) {
      return { ...results, itemsStatus: state };
    } else if (message.indexOf('shipping address') != -1) {
      return { ...results, shippingAddressStatus: state };
    } else if (message.indexOf('shipping method') != -1) {
      return { ...results, shippingMethodStatus: state };
    } else if (message.indexOf('payment method') != -1) {
      return { ...results, paymentMethodStatus: state };
    } else if (message.indexOf('insufficient funds') != -1) {
      return { ...results, paymentMethodStatus: state };
    }

    return results;
  }, {});
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
  },
  validations: {
    errors: [],
    warnings: [],
    itemsStatus: 'success',
    shippingAddressStatus: 'success',
    shippingMethodStatus: 'success',
    paymentMethodStatus: 'success'
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
    const order = _.get(payload, 'result', payload);
    const skus = _.get(order, 'lineItems.skus', []);
    const itemList = collectLineItems(skus);
    const errors = _.get(payload, 'errors', []);
    const warnings = _.get(payload, 'warnings', []);

    // Initial state (assume in good standing)
    const status = {
      itemsStatus: 'success',
      shippingAddressStatus: 'success',
      shippingMethodStatus: 'success',
      paymentMethodStatus: 'success',

      // Find warnings
      ...parseMessages(warnings, 'warning'),

      // Find errors
      ...parseMessages(errors, 'error')
    };

    return {
      ...state,
      isFetching: false,
      currentOrder: new OrderParagon(order),
      lineItems: {
        ...state.lineItems,
        items: itemList
      },
      validations: {
        errors: errors,
        warnings: warnings,
        ...status
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
      ['lineItems', 'items'], collectLineItems(skus)
    );
  },
  [orderLineItemsRequest]: (state, sku) => {
    return assoc(state,
      ['lineItems', 'isUpdating'], true,
      ['lineItems', 'skuToUpdate'], sku
    );
  },
  [orderLineItemsRequestSuccess]: (state, sku) => {
    if (sku === state.lineItems.skuToUpdate) {
      return assoc(state,
        ['lineItems', 'isUpdating'], false,
        ['lineItems', 'isDeleting'], false,
        ['lineItems', 'skuToUpdate'], null,
        ['lineItems', 'skuToDelete'], null
      );
    }

    return state;
  },
  [orderLineItemsRequestFailed]: (state, [err, source]) => {
    console.error(err);

    if (source === updateLineItemCount) {
      return assoc(state,
        ['lineItems', 'isUpdating'], false,
        ['lineItems', 'isDeleting'], false,
        ['lineItems', 'skuToUpdate'], null,
        ['lineItems', 'skuToDelete'], null
      );
    }

    return state;
  },
  [orderLineItemsStartDelete]: (state, sku) => {
    return assoc(state,
      ['lineItems', 'isDeleting'], true,
      ['lineItems', 'skuToDelete'], sku
    );
  },
  [orderLineItemsCancelDelete]: (state, sku) => {
    if (state.lineItems.skuToDelete === sku) {
      return assoc(state,
        ['lineItems', 'isDeleting'], false,
        ['lineItems', 'skuToDelete'], null
      );
    }
  }
}, initialState);

export default reducer;
