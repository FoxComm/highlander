/* @flow weak */

import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyCoupon, configureCoupon} from '../../paragons/coupons';
import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

/* coupon actions */

export const couponsNew = createAction('COUPONS_NEW');
const defaultContext = 'default';

const getCoupon = createAsyncActions(
  'getCoupon',
  (id: string, context = defaultContext) => {
    return Api.get(`/coupons/${context}/${id}`);
  }
);

const _createCoupon = createAsyncActions(
  'createCoupon',
  (coupon, context = defaultContext) => {
    return Api.post(`/coupons/${context}`, coupon);
  }
);

const _updateCoupon = createAsyncActions(
  'updateCoupon',
  (coupon, context = defaultContext) => {
    const id = coupon.form.id;
    return Api.patch(`/coupons/${context}/${id}`, coupon);
  }
);

export function fetchCoupon(id: string, context: string = defaultContext) {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(couponsNew());
    } else {
      dispatch(getCoupon.perform(id, context));
      dispatch(_getCodes.perform(id));
    }
  };
}

export const createCoupon = _createCoupon.perform;
export const updateCoupon = _updateCoupon.perform;
export const generateCode = _generateCode.perform;

export function generateCodes(prefix: string, length: number, quantity: number) {
  return (dispatch, getState) => {
    const id = _.get(getState(), 'coupons.details.coupon.id');
    if (!id) return;

    dispatch(_generateCodes.perform(id, prefix, length, quantity));
  };
}

function updateCouponInState(state, response) {
  return {
    ...state,
    coupon: configureCoupon(response)
  };
}

/* coupon code generation actions */

const _generateCodes = createAsyncActions(
  'generateCouponCodes',
  (id: number, prefix: string, length: number, quantity: number) => {
    return Api.post(`/coupons/codes/generate/${id}`, {
      prefix,
      length: prefix.length + length,
      quantity,
    });
  }
);

const _generateCode = createAsyncActions(
  'generateCouponCode',
  (id: number, code: string) => {
    return Api.post(`/coupons/codes/generate/${id}/${code}`);
  }
);

const _getCodes = createAsyncActions(
  'getCouponCodes',
  (id: number) => {
    return Api.get(`/coupons/codes/${id}`);
  }
);

/* module state */

const initialState = {
  coupon: null,
  codes: [],
  selectedPromotions: [],
  codeGeneration: {
    bulk: void 0,
    codesPrefix: '',
    singleCode: '',
    codesQuantity: 1,
    codesLength: 1,
    isDialogVisible: false,
  }
};

const reducer = createReducer({
  [couponsNew]: state => {
    return {
      ...state,
      coupon: createEmptyCoupon(),
    };
  },
  [getCoupon.succeeded]: updateCouponInState,
  [_createCoupon.succeeded]: updateCouponInState,
  [_updateCoupon.succeeded]: updateCouponInState,
  [_getCodes.succeeded]: (state, codes) => {
    const oldCodes = state.codes || [];
    const newCodes = codes.map(code => code.code);
    return {
      ...state,
      codes: [...oldCodes, ...newCodes]
    };
  },
  [_generateCodes.succeeded]: (state, newCodes) => {
    const oldCodes = state.codes || [];
    return {
      ...state,
      codes: [...oldCodes, ...newCodes]
    };
  },
  [_generateCode.succeeded]: (state, code) => {
    const oldCodes = state.codes || [];
    return {
      ...state,
      codes: [...oldCodes, code]
    };
  },
}, initialState);

export default reducer;
