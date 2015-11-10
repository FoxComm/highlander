import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { update, get } from 'sprout-data';
import { paginate, createFetchActions, pickFetchParams, paginateReducer } from '../pagination';

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

export function fetchRmas(entity={entityType: 'rma'}, newFetchParams) {
  const {entityType, entityId} = entity;

  return (dispatch, getState) => {
    const uri = buildUri(entityType, entityId);
    const state = getStateForEntity(getState(), entityType, entityId);
    const fetchParams = pickFetchParams(state, newFetchParams);

    dispatch(actionFetch(entity));
    dispatch(actionSetFetchParams(entity, newFetchParams));
    return Api.get(uri, fetchParams)
      .then(json => dispatch(actionReceived(entity, json)))
      .catch(err => dispatch(actionFetchFailed(entity, err)));
  };
}

function paginateBehaviour(state, action, actionType) {
  //behaviour for initial state
  if (actionType === void 0) return state;

  const [{entityType, entityId}, payload] = action.payload;

  if (entityId) {
    // For any child rma list eg. /rmas/order/1
    return update(state, [entityType, entityId], paginate, {
      ...action,
      payload,
      type: actionType
    });
  }
  return paginate(undefined, {
    ...action,
    payload,
    type: actionType
  });
}

export default paginateReducer(RMAS, state => state, paginateBehaviour);
