
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyCoupon, configureCoupon} from '../../paragons/coupons';
import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

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
    coupon['promotion'] = 1;
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

const initialState = {
  coupon: null,
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
}, initialState);

export default reducer;
