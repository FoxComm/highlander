import _ from 'lodash';
import { assoc, update, merge, dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_STORE_CREDITS_' + description, ...args);
};

const requestStoreCredits = _createAction('REQUEST');
const receiveStoreCredits = _createAction('RECEIVE', (id, credits) => [id, credits]);
const failStoreCredits = _createAction("FAIL", (id, err) => [id, err]);

const initialState = {};

function storeCreditsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit`;
}

export function fetchStoreCredits(customerId) {
  console.log(customerId);
  return dispatch => {
    dispatch(requestStoreCredits(customerId));
    return Api.get(storeCreditsUrl(customerId))
      .then(storeCredits => {
        dispatch(receiveStoreCredits(customerId, storeCredits));
      })
      .catch(err => {
        dispatch(failStoreCredits(customerId, err));
      });
  };
}

const reducer = createReducer({
  [requestStoreCredits]: (state, id) => {
    return assoc(state, [id, 'isFetching'], true);
  },
  [receiveStoreCredits]: (state, [id, payload]) => {
    console.log(id);
    console.log(payload);
    return assoc(state,
      [id, 'isFetching'], false,
      [id, 'storeCredits'], payload
    );
  },
  [failStoreCredits]: (state, [id, err]) => {
    console.error(err);

    return assoc(state, [id, 'isFetching'], false);
  }
}, initialState);

export default reducer;
