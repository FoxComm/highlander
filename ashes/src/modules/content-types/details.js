import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

export const receiveContentType = createAction('CONTENT_TYPE_RECEIVE', (id, card) => [id, card]);
export const failContentType = createAction('CONTENT_TYPE_FAIL', (id, err, source) => [id, err, source]);
export const requestContentType = createAction('CONTENT_TYPE_REQUEST');
export const updateContentType = createAction('CONTENT_TYPE_UPDATED', (id, card) => [id, card]);
export const changeContentTypeStatus = createAction('CONTENT_TYPE_CHANGE_STATUS', (id, stauts) => [id, stauts]);
export const cancelChangeContentTypeStatus = createAction('CONTENT_TYPE_CANCEL_CHANGE_STATUS');
export const changeCancellationReason = createAction('CONTENT_TYPE_CANCEL_REASON_CAHNGE',
  (id, reasonId) => [id, reasonId]);

function shouldFetchContentType(id, state) {
  const entry = state.contentTypes.details[id];
  if (!entry) {
    return true;
  } else if (entry.isFetching) {
    return false;
  }
  return entry.didInvalidate;
}

export function fetchContentType(id) {
  return dispatch => {
    dispatch(requestContentType(id));
    Api.get(`/content-types/${id}`)
      .then(
        card => dispatch(receiveContentType(id, card)),
        err => dispatch(failContentType(id, err, fetchContentType))
      );
  };
}

export function fetchContentTypeIfNeeded(id) {
  return (dispatch, getState) => {
    if (shouldFetchContentType(id, getState())) {
      dispatch(fetchContentType(id));
    }
  };
}

function sendUpdate(id, data, dispatch) {
  return Api.patch(`/content-types/${id}`, data)
    .then(
      card => dispatch(updateContentType(id, card)),
      err => dispatch(failContentType(id, err, editContentType))
    );
}

export function editContentType(id, data) {
  return dispatch => {
    return sendUpdate(id, data, dispatch);
  };
}

export function saveContentTypeStatus(id) {
  return (dispatch, getStatus) => {
    const status = getStatus();
    const cardData = _.get(status, ['contentTypes', 'details', id]);
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
  [requestContentType]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetching: true,
        didInvalidate: false,
        err: null,
        nextState: null,
        confirmationShown: false,
        reasonId: null,
        card: null,
      }
    };
  },
  [receiveContentType]: (state, [id, card]) => {
    return {
      ...state,
      [id]: {
        err: null,
        isFetching: false,
        didInvalidate: false,
        nextState: null,
        confirmationShown: false,
        reasonId: null,
        card: haveType(card, 'content-type')
      }
    };
  },
  [failContentType]: (state, [id, err, source]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        err,
        ...(
          source === fetchContentType ? {
            isFetching: false,
            didInvalidate: false} : {}
        )
      }
    };
  },
  [updateContentType]: (state, [id, card]) => {
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
  [changeContentTypeStatus]: (state, [id, status]) => {
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
  [cancelChangeContentTypeStatus]: (state, id) => {
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
