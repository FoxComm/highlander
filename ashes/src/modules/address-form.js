
import { assoc, get } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { createAddress, patchAddress } from './customers/addresses';

const setError = createAction('ADDRESS_FORM_SET_ERROR', (form, err) => [form, err]);

export function submitForm(form, customerId, formData) {
  return dispatch => {
    const addressId = get(formData, 'id', null);

    if (!addressId) {
      return dispatch(createAddress(customerId, formData))
        .then(
          address => dispatch(setError(form, null)) && address,
          err => dispatch(setError(form, err)) && err
        );
    } else {
      return dispatch(patchAddress(customerId, addressId, formData))
        .then(
          address => dispatch(setError(form, null)) && address,
          err => dispatch(setError(form, err)) && err
        );
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [setError]: (state, [form, err]) => {
    if (err) console.error(err);

    return assoc(state,
      [form, 'err'], err
    );
  }
}, initialState);

export default reducer;
