
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyPromotion, configurePromotion} from '../../paragons/promotion';
import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

export const promotionsNew = createAction();
const defaultContext = 'default';

const getPromotion = createAsyncActions(
  'getPromotion',
  (id: string, context) => {
    return Api.get(`/promotions/${context}/${id}`);
  }
);

const _createPromotion = createAsyncActions(
  'createPromotion',
  (promotion, context) => {
    return Api.post(`/promotions/${context}`, promotion);
  }
);

export function fetchPromotion(id: string, context: string = defaultContext) {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(promotionsNew());
    } else {
      dispatch(getPromotion.perform(id, context));
    }
  };
}

export const createPromotion = _createPromotion.perform;

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
  [getPromotion.succeeded]: updatePromotionInState,
  [_createPromotion.succeeded]: updatePromotionInState,
}, initialState);

export default reducer;
