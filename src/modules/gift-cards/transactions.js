import _ from 'lodash';
import Api from '../../lib/api';
import { createAction as _createAction, createReducer } from 'redux-act';

const createAction = (desc, ...args) => _createAction(`GIFT_CARDS_TRANSACTIONS_${desc}`, ...args);

export const receiveTransactions = createAction('RECEIVE', (id, transactions) => [id, transactions]);
export const failTransactions = createAction('FAIL', (id, err) => [id, err]);
export const requestTransactions = createAction('REQUEST');

export function fetchTransactions(id) {
  return dispatch => {
    dispatch(requestTransactions(id));
    return Api.get(`/gift-cards/${id}/transactions`)
      .then(
        json => dispatch(receiveTransactions(id, json)),
        err => dispatch(failTransactions(id, err))
      );
  };
}

function shouldFetchTransactions(state, id) {
  const transactions = state.giftCards.transactions[id];
  if (!transactions) {
    return true;
  } else if (transactions.isFetching) {
    return false;
  }
  return transactions.didInvalidate;
}

export function fetchTransactionsIfNeeded(id) {
  return (dispatch, getState) => {
    if (shouldFetchTransactions(getState(), id)) {
      return dispatch(fetchTransactions(id));
    }
  };
}

const initialState = {
};

const reducer = createReducer({
  [requestTransactions]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetching: true,
        didInvalidate: false
      }
    };
  },
  [receiveTransactions]: (state, [id, transactions]) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetching: false,
        didInvalidate: false,
        items: transactions
      }
    };
  },
  [failTransactions]: (state, [id, err]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        isFetching: false,
        didInvalidate: false
      }
    };
  }
}, initialState);

export default reducer;
