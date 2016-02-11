// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import Api from '../../lib/api';


const bulkRequest = createAction('BULK_REQUEST');
const bulkDone = createAction('BULK_DONE', (successes, errors) => [successes, errors]);
const bulkReset = createAction('BULK_RESET');
const bulkClearSuccesses = createAction('BULK_CLEAR_SUCCESSES');
const bulkClearErrors = createAction('BULK_CLEAR_ERRORS');

// TODO remove when https://github.com/FoxComm/phoenix-scala/issues/763 closed
const parseErrors = (errors) => {
  const referenceNumberPattern = /\w{2}\d{5}/;

  return _.transform(errors, (result, value) => {
    const referenceNumber = referenceNumberPattern.exec(value)[0];
    result[referenceNumber] = [value];
  }, {});
};

const getSuccesses = (referenceNumbers, errors = {}) => {
  return referenceNumbers
    .filter(referenceNumber => !(referenceNumber in errors))
    .reduce((result, referenceNumber) => {
      return {
        ...result,
        [referenceNumber]: []
      };
    }, {});
};

export function cancelOrders(referenceNumbers, reasonId) {
  return dispatch => {
    dispatch(bulkRequest());
    Api.patch('/orders', {
        referenceNumbers,
        reasonId,
        state: 'canceled',
      })
      .then(
        ({errors}) => {
          if (errors) {
            errors = parseErrors(errors);
            dispatch(bulkDone(getSuccesses(referenceNumbers, errors), errors));
          } else {
            dispatch(bulkDone(getSuccesses(referenceNumbers)));
          }
        },
        error => {
          // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
          console.error(error);
        }
      );
  };
}

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
      successes: _.size(successes) ? successes : null,
      errors: _.size(errors) ? errors : null,
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
