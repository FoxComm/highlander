/* @flow weak */

import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyCoupon, configureCoupon} from '../../paragons/coupons';
import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

/* coupon actions */

export const couponsNew = createAction('COUPONS_NEW');
export const couponsChange = createAction('COUPONS_CHANGE');

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

function updateCouponInState(state, response) {
  return {
    ...state,
    coupon: configureCoupon(response)
  };
}

/* coupon code generation actions */

export const couponsGenerationReset = createAction('COUPONS_GENERATION_RESET');
export const couponsGenerationShowDialog = createAction('COUPONS_GENERATION_SHOW_DIALOG');
export const couponsGenerationHideDialog = createAction('COUPONS_GENERATION_HIDE_DIALOG');
export const couponsGenerationSelectSingle = createAction('COUPONS_GENERATION_SELECT_SINGLE');
export const couponsGenerationSelectBulk = createAction('COUPONS_GENERATION_SELECT_BULK');
export const couponsGenerationChange = createAction('COUPONS_GENERATION_CHANGE', (name, value) => [name, value]);

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

export const generateCode = _generateCode.perform;

export function generateCodes(prefix: string, length: number, quantity: number) {
  return (dispatch, getState) => {
    const id = _.get(getState(), 'coupons.details.coupon.id');
    if (!id) return;

    dispatch(_generateCodes.perform(id, prefix, length, quantity));
  };
}

export function codeIsOfValidLength(): Function {
  return (dispatch: Function, getState: Function) => {
    const state = getState();
    const quantity = _.get(state, 'coupons.details.codeGeneration.codesQuantity');
    const length = _.get(state, 'coupons.details.codeGeneration.codesLength');
    return length >= Math.ceil(Math.log10(quantity));
  };
}

/* module state */

const initialCodeGenerationState = {
  bulk: void 0,
  codesPrefix: null,
  singleCode: null,
  codesQuantity: 1,
  codesLength: 1,
  isDialogVisible: false,
};

const initialState = {
  coupon: null,
  codes: [],
  selectedPromotions: [],
  codeGeneration: initialCodeGenerationState,
};

const reducer = createReducer({
  [couponsNew]: state => {
    return {
      ...state,
      coupon: createEmptyCoupon(),
    };
  },
  [couponsChange]: (state, coupon) => {
    return {
      ...state,
      coupon,
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
  [couponsGenerationReset]: (state) => {
    return {
      ...state,
      codeGeneration: initialCodeGenerationState,
    };
  },
  [couponsGenerationShowDialog]: (state) => {
    return assoc(state, ['codeGeneration', 'isDialogVisible'], true);
  },
  [couponsGenerationHideDialog]: (state) => {
    return assoc(state, ['codeGeneration', 'isDialogVisible'], false);
  },
  [couponsGenerationSelectSingle]: (state) => {
    return assoc(state, ['codeGeneration', 'bulk'], false);
  },
  [couponsGenerationSelectBulk]: (state) => {
    return assoc(state, ['codeGeneration', 'bulk'], true);
  },
  [couponsGenerationChange]: (state, [name, value]) => {
    return assoc(state, ['codeGeneration', name], value);
  },
}, initialState);

export default reducer;
