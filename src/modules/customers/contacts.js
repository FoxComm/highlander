import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';
import { assoc, dissoc, update, merge, get } from 'sprout-data';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_CONTACTS_' + description, ...args);
};

// UI state actions
export const toggleEditCustomer = _createAction('TOGGLE_EDIT');

// API actions
const customerUpdated = _createAction('UPDATED', (id, customer) => [id, customer]);
const failCustomer = _createAction('FAIL', (id, err) => [id, err]);


export function updateCustomerContacts(id, data) {
  return dispatch => {
    Api.patch(`/customers/${id}`, data)
      .then(customer => dispatch(customerUpdated(id, customer)))
      .catch(err => dispatch(failCustomer(id, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [failCustomer]: (state, [id, err]) => {
    console.error(err);

    return assoc(state,
      [id, 'err'], err,
      [id, 'isUpdating'], false
    );
  },
  [customerUpdated]: (state, [id, details]) => {
    return assoc(state,
      [id, 'details'], haveType(details, 'customer'),
      [id, 'err'], null
    );
  },
  [toggleEditCustomer]: (state, id) => {
    return update(state, [id, 'isContactsEditing'], isEdit => !isEdit);
  }
}, initialState);

export default reducer;