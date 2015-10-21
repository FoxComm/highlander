'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { updateItems } from '../state-helpers';

export const receiveGiftCards = createAction('GIFT_CARDS_RECEIVE');
export const updateGiftCards = createAction('GIFT_CARDS_UPDATE');
export const failGiftCards = createAction('GIFT_CARDS_FAIL', (err, source) => [err, source]);
export const requestGiftCards = createAction('GIFT_CARDS_REQUEST');

export function fetchGiftCards() {
  return dispatch => {
    dispatch(requestGiftCards());
    return Api.get('/gift-cards')
      .then(cards => dispatch(receiveGiftCards(cards)))
      .catch(err => dispatch(failGiftCards(err)));
  };
}

function shouldFetchGiftCards(state) {
  const giftCards = state.giftCards.cards;
  if (!giftCards) {
    return true;
  } else if (giftCards.isFetching) {
    return false;
  }
  return giftCards.didInvalidate;
}

export function fetchGiftCardsIfNeeded() {
  return (dispatch, getState) => {
    if (shouldFetchGiftCards(getState())) {
      return dispatch(fetchGiftCards());
    }
  };
}

export function createGiftCard() {
  return (dispatch, getState) => {
    const { giftCardsNew } = getState();

    // @TODO: select correct data from state for gift card creation
    // please don't ask me for get rid of this todo now, this task is not part of reduxing story.
    Api.post('/gift-cards', giftCardsNew)
      .then(json => dispatch(updateGiftCards([json])))
      .catch(err => dispatch(failGiftCards(err)));
  };
}

const initialState = {
  isFetching: false,
  didInvalidate: true,
  items: []
};

const reducer = createReducer({
  [requestGiftCards]: (state) => {
    return {
      ...state,
      isFetching: true,
      didInvalidate: false
    };
  },
  [receiveGiftCards]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      didInvalidate: false,
      items: payload
    };
  },
  [updateGiftCards]: (state, payload) => {
    return {
      ...state,
      items: updateItems(state.items, payload)
    };
  },
  [failGiftCards]: (state, [err, source]) => {
    console.error(err);

    if (source === fetchGiftCards) {
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    }

    return state;
  }
}, initialState);

export default reducer;
