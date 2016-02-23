import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

const _createAction = (description, ...args) => {
  return createAction(`SHIPPING_ADDRESSES_DEUX_${description}`, ...args);
};

const setError = _createAction('ERROR');
const updateAddressStart = _createAction('UPDATE_ADDRESS_START');
const updateAddressFinish = _createAction('UPDATE_ADDRESS_FINISH');

export function chooseAddress(refNum, addressId) {
  return dispatch => {
    dispatch(updateAddressStart());
    return Api.patch(`/orders/${refNum}/shipping-address/${addressId}`)
      .then (
        order => {
          dispatch(updateAddressFinish());
          dispatch(orderSuccess(order));
        },
        err => {
          dispatch(updateAddressFinish());
          dispatch(setError(err));
        }
      );
  };
}

export function deleteShippingAddress(refNum) {
  return dispatch => {
    dispatch(updateAddressStart());
    return Api.delete(`/orders/${refNum}/shipping-address`)
      .then(
        order => {
          dispatch(updateAddressFinish());
          dispatch(orderSuccess(order));
        },
        err => {
          dispatch(updateAddressFinish());
          dispatch(setError(err));
        }
      );
  };
}

const initialState = {
  err: null,
  isUpdating: false,
};

const reducer = createReducer({
  [setError]: (state, err) => {
    return assoc(state, 'err', err);
  },
  [updateAddressStart]: (state) => {
    return assoc(state, 'isUpdating', true);
  },
  [updateAddressFinish]: (state) => {
    return assoc(state, 'isUpdating', false);
  }
}, initialState);

export default reducer;
