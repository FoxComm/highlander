import _ from 'lodash';
import { assoc, update, merge, dissoc, get } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';
import { paginateReducer, actionTypes, paginate, pickFetchParams, createFetchActions } from '../pagination';

const STORE_CREDIT_TRANSACTIONS = 'STORE_CREDIT_TRANSACTIONS';

const {
  actionFetch,
  actionReceived,
  actionFetchFailed,
  actionSetFetchParams,
  actionAddEntity,
  actionRemoveEntity
  } = createFetchActions(STORE_CREDIT_TRANSACTIONS, (entity, payload) => [entity, payload]);

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_STORE_CREDIT_TRANSACTIONS_' + description, ...args);
};

const failStoreCreditTransactions = _createAction("FAIL", (id, err) => [id, err]);

const initialState = {};

function storeCreditTransactionsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit/transactions`;
}

export function fetchStoreCreditTransactions(entity, newFetchParams) {
  return (dispatch, getState) => {
    const customerId = entity.entityId;
    const state = get(getState(), 'customers');
    const fetchParams = pickFetchParams(state, newFetchParams);

    dispatch(actionFetch(entity));
    dispatch(actionSetFetchParams(entity, newFetchParams));
    Api.get(storeCreditTransactionsUrl(customerId), fetchParams)
      .then(json => dispatch(actionReceived(entity, json)))
      .catch(err => dispatch(actionFetchFailed(entity, err)));
  };
}

const reducer = createReducer({
  [actionReceived]: (state, [{entityType, entityId}, storeCredits]) => {
    return assoc(state, [entityId, 'wasReceived'], true);
  },
  [failStoreCreditTransactions]: (state, [{entityType, entityId}, error]) => {
    console.error(error);

    return assoc(state, entityId, {
      error
    });
  }
}, initialState);

function paginateBehaviour(state, action, actionType) {
  // behaviour for initial state
  if (actionType === void 0) return state;

  const [{entityType, entityId}, payload] = action.payload;

  return update(state, [entityId, entityType], paginate, {
    ...action,
    payload,
    type: actionType
  });
}

export default paginateReducer(STORE_CREDIT_TRANSACTIONS, reducer, paginateBehaviour);
