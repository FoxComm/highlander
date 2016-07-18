import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { orderSuccess } from './details';

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

export function patchShippingAddress(refNum, data) {
  return dispatch => {
    dispatch(updateAddressStart());
    return Api.patch(`/orders/${refNum}/shipping-address`, data)
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

export function createShippingAddress(refNum, data) {
  return dispatch => {
    dispatch(updateAddressStart());
    return Api.post(`orders/${refNum}/shipping-address`, data)
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
  updateNum: 0,
};

const reducer = createReducer({
  [setError]: (state, err) => {
    return assoc(state, 'err', err);
  },
  [updateAddressStart]: (state) => {
    return assoc(state, 'isUpdating', true);
  },
  [updateAddressFinish]: (state) => {
    return assoc(state,
      'isUpdating', false,
      'updateNum', state.updateNum + 1
    );
  }
}, initialState);

export default reducer;
