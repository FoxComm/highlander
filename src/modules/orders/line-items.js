import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';
import { get, assoc } from 'sprout-data';
import OrderParagon from '../../paragons/order';

const _createLineItemAction = (description, ...args) => {
  return createAction('ORDER_LINE_ITEMS_' + description, ...args);
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
            dispatch(orderLineItemsRequestSuccess(sku));
            dispatch(orderLineItemsFetchSuccess(order.result));
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

const initialState = {
  currentSkus: [],
  isEditing: false,
  isUpdating: false,
  isDeleting: false,
  skuToUpdate: null,
  skuToDelete: null,
  items: [],
  validations: {
    errors: [],
    warnings: [],
    itemsStatus: 'success'
  }
};

function determineStatus(warnings, errors) { 
    let state = 'success'
    if(warnings.length > 0) state = 'warning';
    if(errors.length > 0) state = 'error';
    return state;
}

const reducer = createReducer({
  [orderLineItemsFetchSuccess]: (state, payload) => {
    const order = _.get(payload, 'result', payload);
    const skus = _.get(order, 'lineItems.skus', []);
    const itemList = collectLineItems(skus);
    const warnings = _.get(payload, 'warnings', []);
    const errors = _.get(payload, 'errors', []);

    return assoc(state, 
      ['currentSkus'], skus,
      ['isFetching'], false,
      ['validations', 'itemsStatus'], determineStatus(warnings, errors),
      ['validations', 'errors'], errors,
      ['validations', 'warnings'], warnings,
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
