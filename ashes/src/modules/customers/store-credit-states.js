import { assoc, get } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_STORE_CREDITS_STATE_' + description, ...args);
};

export const changeState = _createAction('CHANGE_STARTS',
                                          (customerId, targetId, targetState) => [customerId, targetId, targetState]);
export const cancelChange = _createAction('CANCEL_STATE_CHANGE');
export const reasonChange = _createAction('REASON_CHANGE',
                                          (customerId, reasonId) => [customerId, reasonId]);
const setError = _createAction('FAIL', (customerId, err) => [customerId, err]);

const initialState = {};

function updateStoreCreditsUrl(scId) {
  return `/store-credits/${scId}`;
}

export function saveStateChange(customerId) {
  return (dispatch, getState) => {
    const creditToChange = get(getState(), ['customers', 'storeCreditStates', customerId, 'storeCreditToChange']);
    const payload = {
      state: creditToChange.state,
      reasonId: creditToChange.reasonId
    };

    Api.patch(updateStoreCreditsUrl(creditToChange.targetId), payload)
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
