
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, update, merge, dissoc } from 'sprout-data';

const _createAction = (description, ...args) => {
  return createAction(`SHIPPING_ADDRESSES_${description}`, ...args);
};

export const startEditing = _createAction('START_EDITING');
export const stopEditing = _createAction('STOP_EDITING');

export const startAddingAddress = _createAction('START_ADDING_ADDRESS');

export const startEditingAddress = _createAction('START_EDITING_ADDRESS');
export const stopAddingOrEditingAddress = _createAction('STOP_ADDING_OR_EDITING');

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
  }
}, initialState);

export default reducer;
