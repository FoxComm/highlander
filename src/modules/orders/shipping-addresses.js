
import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, update, merge, dissoc } from 'sprout-data';

import { fetchOrder } from './details';

const _createAction = (description, ...args) => {
  return createAction(`SHIPPING_ADDRESSES_${description}`, ...args);
};

export const startEditing = _createAction('START_EDITING');
export const stopEditing = _createAction('STOP_EDITING');

export const startAddingAddress = _createAction('START_ADDING_ADDRESS');

export const startEditingAddress = _createAction('START_EDITING_ADDRESS');
export const stopAddingOrEditingAddress = _createAction('STOP_ADDING_OR_EDITING');

export const startDeletingAddress = _createAction('START_DELETING');
export const stopDeletingAddress = _createAction('STOP_DELETING');

export function chooseAddress(refNum, addressId) {
  return dispatch => {
    return Api.patch(`/orders/${refNum}/shipping-address`, {addressId})
      .then(ok => dispatch(fetchOrder(refNum)));
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
  [startEditingAddress]: (state, addressId) => {
    return assoc(state, 'editingId', addressId);
  },
  [stopAddingOrEditingAddress]: state => {
    return dissoc(state, 'editingId');
  },
  [startDeletingAddress]: (state, addressId) => {
    return assoc(state, 'deletingId', addressId);
  },
  [stopDeletingAddress]: state => {
    return dissoc(state, 'deletingId');
  }
}, initialState);

export default reducer;
