
import { dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyPromotion } from 'paragons/promotion';
import { createAsyncActions } from '@foxcomm/wings';
import Api from 'lib/api';

export const promotionNew = createAction('PROMOTIONS_NEW');
const clearPromotion = createAction('PROMOTION_CLEAR');
const defaultContext = 'default';

const _fetchPromotion = createAsyncActions(
  'fetchPromotion',
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
    const id = promotion.id;
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
      dispatch(promotionNew());
    } else {
      return dispatch(_fetchPromotion.perform(id, context));
    }
  };
}

const _archivePromotion = createAsyncActions(
  'archivePromotion',
  (id, context = defaultContext) => {
    return Api.delete(`/promotions/${context}/${id}`);
  }
);

export const clearArchiveErrors = _archivePromotion.clearErrors;
export const archivePromotion = _archivePromotion.perform;
export const createPromotion = _createPromotion.perform;
export const updatePromotion = _updatePromotion.perform;
export const clearFetchErrors = _fetchPromotion.clearErrors;

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
    promotion: response
  };
}

const initialState = {
  promotion: null,
};

const reducer = createReducer({
  [promotionNew]: state => {
    return {
      ...state,
      promotion: createEmptyPromotion(),
    };
  },
  [clearPromotion]: state => {
    return dissoc(state, 'promotion');
  },
  [_fetchPromotion.succeeded]: updatePromotionInState,
  [_createPromotion.succeeded]: updatePromotionInState,
  [_updatePromotion.succeeded]: updatePromotionInState,
}, initialState);

export default reducer;
