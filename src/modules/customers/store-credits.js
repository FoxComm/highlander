import _ from 'lodash';
import { assoc, update, merge, dissoc, get } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';
import { paginateReducer, actionTypes, paginate, pickFetchParams, createFetchActions } from '../pagination';

const STORE_CREDITS = 'STORE_CREDITS';

const {
  actionFetch,
  actionReceived,
  actionFetchFailed,
  actionSetFetchParams,
  actionAddEntity,
  actionRemoveEntity
  } = createFetchActions(STORE_CREDITS, (entity, payload) => [entity, payload]);

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_STORE_CREDITS_' + description, ...args);
};

export const changeStatus = _createAction('STATUS_CHANGE',
                                          (customerId, targetId, targetStatus) => [customerId, targetId, targetStatus]);
export const cancelChange = _createAction('CANCEL_STATUS_CHANGE');
export const reasonChange = _createAction('REASON_CHANGE',
                                          (customerId, reasonId) => [customerId, reasonId]);
const updateStoreCredits = _createAction('UPDATE',
                                         (customerId, scId, data) => [customerId, scId, data]);
const failStoreCredits = _createAction('FAIL', (id, err) => [id, err]);

const initialState = {};

function storeCreditsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit`;
}

function updateStoreCreditsUrl(scId) {
  return `/store-credits/${scId}`;
}

export function fetchStoreCredits(entity, newFetchParams) {
  return (dispatch, getState) => {
    const customerId = entity.entityId;
    const state = get(getState(), 'customers');
    const fetchParams = pickFetchParams(state, newFetchParams);

    dispatch(actionFetch(entity));
    dispatch(actionSetFetchParams(entity, newFetchParams));
    Api.get(storeCreditsUrl(customerId), fetchParams)
      .then(json => dispatch(actionReceived(entity, json)))
      .catch(err => dispatch(actionFetchFailed(entity, err)));
  };
}

export function saveStatusChange(entity,) {
  return (dispatch, getState) => {
    const customerId = entity.entityId;
    const creditToChange = get(getState(), ['customers', 'storeCredits', customerId, 'storeCreditToChange']);

    Api.patch(updateStoreCreditsUrl(creditToChange.id), creditToChange)
      .then(json => {
        dispatch(cancelChange(customerId));
        dispatch(updateStoreCredits(customerId, creditToChange.id, json));
      })
      .catch(err => dispatch(actionFetchFailed(entity, err)));
  };
}

const reducer = createReducer({
  [actionReceived]: (state, [{entityType, entityId}, storeCredits]) => {
    return assoc(state, [entityId, 'wasReceived'], true);
  },
  [updateStoreCredits]: (state, [customerId, scId, data]) => {
    return update(state,
      [customerId, 'storeCredits', 'rows'], storeCredits => {
      const index = _.findIndex(storeCredits, {id: scId});

      return update(storeCredits, index, merge, data);
    });
  },
  [changeStatus]: (state, [customerId, targetId, targetStatus]) => {
    const storeCredits = get(state, [customerId, 'storeCredits', 'rows']);
    const creditToChange = _.find(storeCredits, {id: targetId} );
    const preparedToChange = {
      ...creditToChange,
      status: targetStatus
    };

    return assoc(state, [customerId, 'storeCreditToChange'], preparedToChange);
  },
  [cancelChange]: (state, customerId) => {
    return assoc(state, [customerId, 'storeCreditToChange'], null);
  },
  [reasonChange]: (state, [customerId, reasonId]) => {
    const creditToChange = get(state, [customerId, 'storeCreditToChange']);
    const updated = {
      ...creditToChange,
      reasonId: parseInt(reasonId)
    }
    return assoc(state, [customerId, 'storeCreditToChange'], updated);
  },
  [failStoreCredits]: (state, [{entityType, entityId}, error]) => {
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

export default paginateReducer(STORE_CREDITS, reducer, paginateBehaviour);
