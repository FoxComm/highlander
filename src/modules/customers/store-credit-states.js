import _ from 'lodash';
import { assoc, update, merge, dissoc, get } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';

const dataNamespace = ['customers', 'storeCreditStates'];
const dataPath = customerId => [customerId, 'storeCreditToChange'];

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_STORE_CREDITS_STATE_' + description, ...args);
};

export const changeState = _createAction('CHANGE_STARTS',
                                          (customerId, targetId, targetState) => [customerId, targetId, targetState]);
export const cancelChange = _createAction('CANCEL_STATE_CHANGE');
export const reasonChange = _createAction('REASON_CHANGE',
                                          (customerId, reasonId) => [customerId, reasonId]);
const updateStoreCredits = _createAction('UPDATE',
                                         (customerId, scId, data) => [customerId, scId, data]);
const setError = _createAction('FAIL', (customerId, err) => [customerId, err]);

const initialState = {};

function storeCreditsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit`;
}

function updateStoreCreditsUrl(scId) {
  return `/store-credits/${scId}`;
}

export function saveStateChange(customerId) {
  return (dispatch, getState) => {
    const creditToChange = get(getState(), ['customers', 'storeCreditStates', customerId, 'storeCreditToChange']);

    Api.patch(updateStoreCreditsUrl(creditToChange.targetId), creditToChange)
      .then(
        json => {
          dispatch(cancelChange(customerId));
        },
        err => dispatch(setError(customerId, err))
      );
  };
}

const reducer = createReducer({
  [changeState]: (state, [customerId, targetId, targetState]) => {
    const creditToChange = get(state, [customerId, 'storeCreditToChange']);
    const preparedToChange = {
      ...creditToChange,
      targetId,
      state: targetState
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
    console.error(err);

    return state;
  }
}, initialState);

export {
  reducer as default
};
