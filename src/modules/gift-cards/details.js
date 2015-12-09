import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

export const receiveGiftCard = createAction('GIFT_CARD_RECEIVE', (id, card) => [id, card]);
export const failGiftCard = createAction('GIFT_CARD_FAIL', (id, err, source) => [id, err, source]);
export const requestGiftCard = createAction('GIFT_CARD_REQUEST');
export const updateGiftCard = createAction('GIFT_CARD_UPDATED', (id, card) => [id, card]);

function shouldFetchGiftCard(id, state) {
  const entry = state.giftCards.details[id];
  if (!entry) {
    return true;
  } else if (entry.isFetching) {
    return false;
  }
  return entry.didInvalidate;
}

export function fetchGiftCard(id) {
  return dispatch => {
    dispatch(requestGiftCard(id));
    Api.get(`/gift-cards/${id}`)
      .then(
        card => dispatch(receiveGiftCard(id, card)),
        err => dispatch(failGiftCard(id, err, fetchGiftCard))
      );
  };
}

export function fetchGiftCardIfNeeded(id) {
  return (dispatch, getState) => {
    if (shouldFetchGiftCard(id, getState())) {
      dispatch(fetchGiftCard(id));
    }
  };
}

export function editGiftCard(id, data) {
  return dispatch => {
    Api.patch(`/gift-cards/${id}`, data)
      .then(
        card => dispatch(updateGiftCard(id, card)),
        err => dispatch(failGiftCard(id, err, editGiftCard))
      );
  };
}

const initialState = {};

const reducer = createReducer({
  [requestGiftCard]: (entries, id) => {
    return {
      ...entries,
      [id]: {
        ...entries[id],
        isFetching: true,
        didInvalidate: false,
        err: null
      }
    };
  },
  [receiveGiftCard]: (state, [id, card]) => {
    return {
      ...state,
      [id]: {
        err: null,
        isFetching: false,
        didInvalidate: false,
        card: haveType(card, 'gift-card')
      }
    };
  },
  [failGiftCard]: (state, [id, err, source]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        err,
        ...(
          source === fetchGiftCard ? {
            isFetching: false,
            didInvalidate: false} : {}
        )
      }
    };
  },
  [updateGiftCard]: (state, [id, card]) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        err: null,
        card
      }
    };
  }
}, initialState);

export default reducer;
