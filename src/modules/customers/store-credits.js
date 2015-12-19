import _ from 'lodash';
import { assoc, update, merge, dissoc, get } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';

import makePagination from '../pagination/structured-store';

const dataNamespace = ['customers', 'storeCredits'];
const dataPath = customerId => [customerId, 'storeCredits'];

const { makeActions, makeReducer } = makePagination(dataNamespace, dataPath);


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
const setError = _createAction('FAIL', (customerId, err) => [customerId, err]);

const {
  fetch,
  actionReceived
} = makeActions(storeCreditsUrl);

const initialState = {};

function storeCreditsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit`;
}

function updateStoreCreditsUrl(scId) {
  return `/store-credits/${scId}`;
}

export function saveStatusChange(customerId) {
  return (dispatch, getState) => {
    const creditToChange = get(getState(), ['customers', 'storeCredits', customerId, 'storeCreditToChange']);

    Api.patch(updateStoreCreditsUrl(creditToChange.id), creditToChange)
      .then(
        json => {
          dispatch(cancelChange(customerId));
          dispatch(updateStoreCredits(customerId, creditToChange.id, json));
        },
        err => dispatch(setError(customerId, err))
      );
  };
}

const moduleReducer = createReducer({
  [actionReceived]: (state, [customerId, payload]) => {
    return assoc(state,
      [...dataPath(customerId), 'rows'], payload.result.storeCredits,
      [...dataPath(customerId), 'totals'], payload.result.totals
    );
  },
  [updateStoreCredits]: (state, [customerId, scId, data]) => {
    return update(state,
      [...dataPath(customerId), 'rows'], storeCredits => {
      const index = _.findIndex(storeCredits, {id: scId});

      return update(storeCredits, index, merge, data);
    });
  },
  [changeStatus]: (state, [customerId, targetId, targetStatus]) => {
    const storeCredits = get(state, [...dataPath(customerId), 'rows']);
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
    };
    return assoc(state, [customerId, 'storeCreditToChange'], updated);
  },
  [setError]: (state, [customerId, err]) => {
    console.log(err);

    return state;
  }
}, initialState);

const reducer = makeReducer(moduleReducer);

export {
  reducer as default,
  fetch as fetchStoreCredits
};
