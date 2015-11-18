
import _ from 'lodash';
import { assoc, update, merge, dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import Api from '../../lib/api';
import { haveType } from '../state-helpers';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_ADDRESSES_' + description, ...args);
};

// ui state actions
export const startAddingAddress = _createAction('START_ADDING');
export const stopAddingAddress = _createAction('STOP_ADDING');

export const startEditingAddress = _createAction('START_EDITING');
export const stopEditingAddress = _createAction('STOP_EDITING');


const initialState = {
  isAdding: false,
  editingIds: []
};

const reducer = createReducer({
  [startAddingAddress]: state => {
    return assoc(state, 'isAdding', true);
  },
  [stopAddingAddress]: state => {
    return dissoc(state, 'isAdding');
  },
  [startEditingAddress]: (state, addressId) => {
    return update(state, 'editingIds', (ids = []) => {
      return [...ids, addressId];
    });
  },
  [stopEditingAddress]: (state, addressId) => {
    return update(state, 'editingIds', _.without, addressId);
  }
}, initialState);

export default reducer;
