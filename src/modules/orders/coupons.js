
/* @flow weak */

import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';
import _ from 'lodash';
import { orderSuccess } from './details.js';

export type CouponModuleProps = {
  isEditing: boolean,
  code: ?string,
  error: ?string,
};

export type CouponModuleActions = {
  orderCouponCodeChange: Function,
  orderCouponsStartEdit: Function,
  orderCouponsStopEdit: Function,
  addCoupon: Function,
  removeCoupon: Function,
};

const _createAction = (description, ...args) => {
  return createAction('ORDER_COUPON_' + description, ...args);
};

export const orderCouponsStartEdit = _createAction('START_EDIT');
export const orderCouponsStopEdit = _createAction('STOP_EDIT');
export const orderCouponCodeChange = _createAction('CODE_CHANGE');
export const orderCouponApplyError = _createAction('APPLY_ERROR');

function basePath(refNum) {
  return `/orders/${refNum}/coupon`;
}

export function addCoupon(orderRefNum: string) {
  return (dispatch, getState) => {
    const state = getState();
    const code = _.get(state, 'orders.coupons.code');
    return Api.post(`${basePath(orderRefNum)}/${code}`, null)
      .then(
        order => {
          dispatch(orderCouponsStopEdit());
          dispatch(orderSuccess(order));
        },
        err => {
          dispatch(orderCouponApplyError(err));
        }
      );
  };
}

export function removeCoupon(orderRefNum: string) {
  return dispatch => {
    return Api.delete(`${basePath(orderRefNum)}`)
      .then(
        order => {
          dispatch(orderSuccess(order));
        },
        err => console.log(err)
      );
  };
}

const initialState: CouponModuleProps = {
  isEditing: false,
  code: null,
  error: null,
};

const reducer = createReducer({
  [orderCouponCodeChange]: (state, code) => {
    return {
      ...state,
      code,
      error: null,
    };
  },
  [orderCouponsStartEdit]: (state) => {
    return {
      ...state,
      isEditing: true,
      error: null,
    };
  },
  [orderCouponsStopEdit]: (state) => {
    return initialState;
  },
  [orderCouponApplyError]: (state, err) => {
    const errorMessage = _.get(err, ['responseJson', 'errors', 0], 'Unexpected error occured');
    console.error(err.responseJson);
    return {
      ...state,
      error: errorMessage,
    };
  },
}, initialState);

export default reducer;
