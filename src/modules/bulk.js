// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';


export const bulkRequest = createAction('BULK_REQUEST');
export const bulkDone = createAction('BULK_DONE', (successes, errors) => [successes, errors]);
export const bulkReset = createAction('BULK_RESET');
export const bulkClearSuccesses = createAction('BULK_CLEAR_SUCCESSES');
export const bulkClearErrors = createAction('BULK_CLEAR_ERRORS');

export function reset() {
  return dispatch => {
    dispatch(bulkReset());
  };
}

export function clearSuccesses() {
  return dispatch => {
    dispatch(bulkClearSuccesses());
  };
}

export function clearErrors() {
  return dispatch => {
    dispatch(bulkClearErrors());
  };
}

const initialState = {
  isFetching: false,
};

const reducer = createReducer({
  [bulkRequest]: () => {
    return {
      isFetching: true,
    };
  },
  [bulkDone]: (state, [successes, errors]) => {
    return {
      isFetching: false,
      successes: _.isEmpty(successes) ? null : successes,
      errors: _.isEmpty(errors) ? null : errors,
    };
  },
  [bulkReset]: () => {
    return {
      isFetching: false,
    };
  },
  [bulkClearSuccesses]: (state) => {
    return _.omit(state, 'successes');
  },
  [bulkClearErrors]: (state) => {
    return _.omit(state, 'errors');
  },
}, initialState);

export default reducer;
