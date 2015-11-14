
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';

const _createAction = (description, ...args) => {
  return createAction(`ORDER_SHIPPING_ADDRESS_${description}`, ...args);
};

export const startEditing = _createAction('START_EDITING');
export const stopEditing = _createAction('STOP_EDITING');

const initialState = {
  isEditing: false
};

const reducer = createReducer({
  [startEditing]: state => {
    return assoc(state, 'isEditing', true);
  },
  [stopEditing]: state => {
    return assoc(state, 'isEditing', false);
  }
}, initialState);

export default reducer;

