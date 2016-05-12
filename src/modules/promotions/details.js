
import _ from 'lodash';
import { assoc, dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyPromotion, configurePromotion} from '../../paragons/promotion';
import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

export const promotionsNew = createAction();
const clearPromotion = createAction();
const defaultContext = 'default';

const _getPromotion = createAsyncActions(
  'getPromotion',
  (id: string, context) => {
    return Api.get(`/promotions/${context}/${id}`);
  }
);

const _createPromotion = createAsyncActions(
  'createPromotion',
  (promotion, context = defaultContext) => {
    return Api.post(`/promotions/${context}`, promotion);
  }
);

const _updatePromotion = createAsyncActions(
  'updatePromotion',
  (promotion, context = defaultContext) => {
    const id = promotion.form.id;
    return Api.patch(`/promotions/${context}/${id}`, promotion);
  }
);

export function clearSubmitErrors() {
  return dispatch => {
    dispatch(_createPromotion.clearErrors());
    dispatch(_updatePromotion.clearErrors());
  };
}

export function fetchPromotion(id: string, context: string = defaultContext) {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(promotionsNew());
    } else {
      dispatch(_getPromotion.perform(id, context));
    }
  };
}

export const createPromotion = _createPromotion.perform;
export const updatePromotion = _updatePromotion.perform;
export const clearFetchErrors = _getPromotion.clearErrors;

export function reset() {
  return dispatch => {
    dispatch(clearPromotion());
    dispatch(clearSubmitErrors());
    dispatch(clearFetchErrors());
  };
}

function updatePromotionInState(state, response) {
  return {
    ...state,
    promotion: configurePromotion(response)
  };
}

const initialState = {
  promotion: null,
};

const reducer = createReducer({
  [promotionsNew]: state => {
    return {
      ...state,
      promotion: createEmptyPromotion(),
    };
  },
  [clearPromotion]: state => {
    return dissoc(state, 'promotion');
  },
  [_getPromotion.succeeded]: updatePromotionInState,
  [_createPromotion.succeeded]: updatePromotionInState,
  [_updatePromotion.succeeded]: updatePromotionInState,
}, initialState);

export default reducer;
