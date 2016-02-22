// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import Api from '../../lib/api';
import { singularize } from 'fleck';

// data
import reducer, { bulkRequest, bulkDone } from '../bulk';

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
        ({errors = []}) => {
          errors = parseErrors(errors);
          dispatch(bulkDone(getSuccesses(referenceNumbers, errors), errors));
        },
        error => {
          // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
          console.error(error);
        }
      );
  };
}

export function changeOrdersState(referenceNumbers, state) {
  return dispatch => {
    dispatch(bulkRequest());
    Api.patch('/orders', {
        referenceNumbers,
        state,
      })
      .then(
        ({errors = []}) => {
          errors = parseErrors(errors);
          dispatch(bulkDone(getSuccesses(referenceNumbers, errors), errors));
        },
        error => {
          // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
          console.error(error);
        }
      );
  };
}

const toggleWatchOrders = watch => (group, referenceNumbers, watchers) => dispatch => {
  const groupMember = singularize(group);

  dispatch(bulkRequest());

  const url = watch ? `/orders/${group}` : `/orders/${group}/delete`;

  Api.post(url, {
      referenceNumbers,
      [`${groupMember}Id`]: watchers[0],
    })
    .then(
      ({errors = []}) => {
        errors = parseErrors(errors);
        dispatch(bulkDone(getSuccesses(referenceNumbers, errors), errors));
      },
      error => {
        // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
        console.error(error);
      }
    );
};

export const watchOrders = toggleWatchOrders(true);
export const unwatchOrders = toggleWatchOrders(false);


export default reducer;
export { reset, clearSuccesses, clearErrors } from '../bulk';
