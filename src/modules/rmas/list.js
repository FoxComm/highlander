'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const requestRmas = createAction('RMAS_REQUEST', (type, identifier) => [type, identifier]);
export const receiveRmas = createAction('RMAS_RECEIVE', (payload, type, identifier) => [payload, type, identifier]);
export const updateRmas = createAction('RMAS_UPDATE', (payload, type, identifier) => [payload, type, identifier]);
export const failRmas = createAction('RMAS_FAIL', (err, source, type, identifier) => [err, source, type, identifier]);

function performFetch(source, url, type, identifier) {
  return dispatch => {
    dispatch(requestRmas(type, identifier));
    return Api.get(url)
      .then(rmas => dispatch(receiveRmas(rmas, type, identifier)))
      .catch(err => dispatch(failRmas(err, source, type, identifier)));
  };
}

export function fetchRmas() {
  return dispatch => {
    dispatch(performFetch(fetchRmas, '/rmas'));
  };
}

export function fetchChildRmas(entity) {
  return dispatch => {
    let identifier;
    let type = entity.entityType;
    if (type === 'order') {
      identifier = entity.referenceNumber;
    } else {
      identifier = entity.id;
    }
    const url = `/rmas/${type}/${identifier}`;
    dispatch(performFetch(fetchChildRmas, url, type, identifier));
  };
}

function updateItems(items, newItems) {
  return _.values({
    ..._.indexBy(items, 'id'),
    ..._.indexBy(newItems, 'id')
  });
}

const initialState = {
  isFetching: false,
  items: [],
  orderRmas: {},
  customerRmas: {}
};

const reducer = createReducer({
  [requestRmas]: (state, [type, identifier]) => {
    let newState = {...state};
    const result = {isFetching: true};
    switch(type) {
      case 'order':
        newState.orderRmas[identifier] = result;
        break;
      case 'customer':
        newState.customerRmas[identifier] = result;
        break;
      default:
        newState = {
          ...newState,
          ...result
        };
    }

    return newState;
  },
  [receiveRmas]: (state, [payload, type, identifier]) => {
    let newState = {...state};
    const result = {
      isFetching: false,
      items: payload.result
    };
    switch(type) {
      case 'order':
        newState.orderRmas[identifier] = result;
        break;
      case 'customer':
        newState.customerRmas[identifier] = result;
        break;
      default:
        newState = {
          ...state,
          ...result
        };
    }
    return newState;
  },
  [updateRmas]: (state, [payload, type, identifier]) => {
    const newState = {...state};
    let node;
    switch(type) {
      case 'order':
        node = state.orderRmas[identifier];
        node.items = updateItems(node.items, payload.result);
      case 'customer':
        node = state.customerRmas[identifier];
        node.items = updateItems(node.items, payload.result);
      default:
        newState.items = updateItems(state.items, payload.result);
    }

    return newState;
  },
  [failRmas]: (state, [err, source, type, identifier]) => {
    console.error(err);

    if (source === fetchRmas) {
      return {
        ...state,
        isFetching: false
      };
    } else if (source === fetchChildRmas) {
      let newState = {...state};
      if (type === 'order') {
        newState.orderRmas[identifier].isFetching = false;
      } else if (type === 'customer') {
        newState.customerRmas[identifier].isFetching = false;
      }
      return newState;
    }

    return state;
  }
}, initialState);

export default reducer;
