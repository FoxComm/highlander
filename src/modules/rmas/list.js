'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, merge, get } from 'sprout-data';
import { paginateReducer, pickFetchParams, createFetchActions, DEFAULT_PAGE_SIZE } from '../pagination';

const RMAS = 'RMAS';
const {
  actionFetch,
  actionReceived,
  actionFetchFailed,
  actionSetFetchParams,
  actionAddEntity,
  actionRemoveEntity
} = createFetchActions(RMAS, (entity, payload) => [entity, payload]);

function buildUri(entityType, entityId) {
  if (entityId) {
    return `/rmas/${entityType}/${entityId}`;
  }
  return '/rmas';
}

function getStateForEntity(state, entityType, entityId) {
  if (entityId) {
    return get(state, [entityType, entityId, 'items']);
  }
  return get(state, ['items']);
}

function fetchRmas(entity, extraFetchParams) {
  const {entityType, entityId} = entity;

  return (dispatch, getState) => {
    const uri = buildUri(entityType, entityId);
    const state = getStateForEntity(getState(), entityType, entityId);
    const fetchParams = pickFetchParams(state, extraFetchParams);

    dispatch(actionSetFetchParams(entity, fetchParams));
    dispatch(actionFetch(entity));
    return Api.get(uri, fetchParams)
      .then(json => dispatch(actionReceived(entity, json)))
      .catch(err => dispatch(actionFetchFailed(entity, err)));
  };
}

function setFetchParams(entity, state, fetchParams) {
  const {entityType, entityId} = entity;

  return dispatch => {
    dispatch(actionSetFetchParams(fetchParams));
    dispatch(fetchRmas(entity, {
      ...state,
      ...fetchParams
    }));
  };
};

const paginationState = {
  rows: [],
  total: 0,
  from: 0,
  size: DEFAULT_PAGE_SIZE
};

const initialState = {
  isFetching: false,
  order: {},
  customer: {},
  ...paginationState
};

const reducer = createReducer({
  [actionFetch]: (state, [{entityType, entityId}]) => {
    if (entityId) {
      const response = merge({...state}, {
        [entityType]: {
          [entityId]: {
            ...paginationState,
            isFetching: true
          }
        }
      });
      return response;
    }
    return {
      ...state,
      isFetching: true
    };
  },
  [actionReceived]: (state, [{entityType, entityId}, payload]) => {
    if (entityId) {
      return assoc(state, [entityType, entityId], {
          ...state[entityType][entityId],
        isFetching: false,
        rows: get(payload, 'result', payload),
        total: get(payload, ['pagination', 'total'], payload.length)
      });
    }
    return {
      ...state,
      isFetching: false,
      rows: get(payload, 'result', payload),
      total: get(payload, ['pagination', 'total'], payload.length)
    };
  },
  [actionFetchFailed]: (state, [{entityType, entityId}]) => {
    if (entityId) {
      return assoc(state, [entityType, entityId, 'isFetching'], false);
    }
    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export {
  fetchRmas,
  setFetchParams,
  paginationState
};

export default reducer;
