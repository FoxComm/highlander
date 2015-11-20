
import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, update, merge, dissoc } from 'sprout-data';

import { orderSuccess, fetchOrder } from './details';

const _createAction = (description, ...args) => {
  return createAction(`SHIPPING_ADDRESSES_${description}`, ...args);
};

export const addressTypes = {
  CUSTOMER: 1,
  SHIPPING: 2
};

export const startEditing = _createAction('START_EDITING');
export const stopEditing = _createAction('STOP_EDITING');

export const startAddingAddress = _createAction('START_ADDING_ADDRESS');

export const startEditingAddress = _createAction('START_EDITING_ADDRESS',
  (addressId, addressType = addressTypes.CUSTOMER) => [addressId, addressType]);
export const stopAddingOrEditingAddress = _createAction('STOP_ADDING_OR_EDITING');

export const startDeletingAddress = _createAction('START_DELETING',
  (addressId, addressType = addressTypes.CUSTOMER) => [addressId, addressType]);
export const stopDeletingAddress = _createAction('STOP_DELETING');

export function chooseAddress(refNum, addressId) {
  return dispatch => {
    return Api.patch(`/orders/${refNum}/shipping-address/${addressId}`)
      .then(order => dispatch(orderSuccess(order)));
  };
}

export function deleteShippingAddress(refNum) {
  return dispatch => {
    return Api.delete(`/orders/${refNum}/shipping-address`)
      .then(ok => dispatch(fetchOrder(refNum)));
  };
}

export function patchShippingAddress(refNum, data) {
  return dispatch => {
    return Api.patch(`/orders/${refNum}/shipping-address`, data)
      .then(order => dispatch(orderSuccess(order)));
  };
}

const initialState = {
  isEditing: false
};

const reducer = createReducer({
  [startEditing]: state => {
    return assoc(state, 'isEditing', true);
  },
  [stopEditing]: state => {
    return assoc(state, 'isEditing', false);
  },
  [startAddingAddress]: state => {
    // -1 means that we add address
    return assoc(state, 'editingId', -1);
  },
  [startEditingAddress]: (state, [id, type]) => {
    return {
      ...state,
      editingAddress: {
        id,
        type
      }
    };
  },
  [stopAddingOrEditingAddress]: state => {
    return dissoc(state, 'editingAddress');
  },
  [startDeletingAddress]: (state, [id, type]) => {
    return {
      ...state,
      deletingAddress: {
        id,
        type
      }
    };
  },
  [stopDeletingAddress]: state => {
    return dissoc(state, 'deletingAddress');
  }
}, initialState);

export default reducer;
