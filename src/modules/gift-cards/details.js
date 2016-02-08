import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

export const receiveGiftCard = createAction('GIFT_CARD_RECEIVE', (id, card) => [id, card]);
export const failGiftCard = createAction('GIFT_CARD_FAIL', (id, err, source) => [id, err, source]);
export const requestGiftCard = createAction('GIFT_CARD_REQUEST');
export const updateGiftCard = createAction('GIFT_CARD_UPDATED', (id, card) => [id, card]);
export const changeGiftCardStatus = createAction('GIFT_CARD_CHANGE_STATUS', (id, stauts) => [id, stauts]);
export const cancelChangeGiftCardStatus = createAction('GIFT_CARD_CANCEL_CHANGE_STATUS');
export const changeCancellationReason = createAction('GIFT_CARD_CANCEL_REASON_CAHNGE',
  (id, reasonId) => [id, reasonId]);

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

function sendUpdate(id, data, dispatch) {
  return Api.patch(`/gift-cards/${id}`, data)
    .then(
      card => dispatch(updateGiftCard(id, card)),
      err => dispatch(failGiftCard(id, err, editGiftCard))
    );
}

export function editGiftCard(id, data) {
  return dispatch => {
    return sendUpdate(id, data, dispatch);
  };
}

export function saveGiftCardStatus(id) {
  return (dispatch, getStatus) => {
    const status = getStatus();
    const cardData = _.get(status, ['giftCards', 'details', id]);
    if (!_.isEmpty(cardData)) {
      const payload = {
        state: cardData.nextState,
        reasonId: cardData.reasonId,
      };
      return sendUpdate(id, payload, dispatch);
    }
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
        err: null,
        nextState: null,
        confirmationShown: false,
        reasonId: null,
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
        nextState: null,
        confirmationShown: false,
        reasonId: null,
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
        nextState: null,
        confirmationShown: false,
        reasonId: null,
        card
      }
    };
  },
  [changeGiftCardStatus]: (state, [id, status]) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        err: null,
        nextState: status,
        confirmationShown: true,
        reasonId: null,
      }
    };
  },
  [cancelChangeGiftCardStatus]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        nextState: null,
        confirmationShown: false,
        reasonId: null,
      }
    };
  },
  [changeCancellationReason]: (state, [id, reasonId]) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        reasonId: reasonId,
      }
    };
  }
}, initialState);

export default reducer;
