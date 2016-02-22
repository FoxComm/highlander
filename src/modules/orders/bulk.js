// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import Api from '../../lib/api';
import { singularize } from 'fleck';
import createStore from '../../lib/store-creator';

// data
import { initialState, reducers } from '../bulk';

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

const cancelOrders = (actions, referenceNumbers, reasonId) =>
  dispatch => {
    dispatch(actions.bulkRequest());
    Api.patch('/orders', {
        referenceNumbers,
        reasonId,
        state: 'canceled',
      })
      .then(
        ({errors = []}) => {
          errors = parseErrors(errors);
          dispatch(actions.bulkDone(getSuccesses(referenceNumbers, errors), errors));
        },
        error => {
          // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
          console.error(error);
        }
      );
  };

const changeOrdersState = (actions, referenceNumbers, state) =>
  dispatch => {
    dispatch(actions.bulkRequest());
    Api.patch('/orders', {
        referenceNumbers,
        state,
      })
      .then(
        ({errors = []}) => {
          errors = parseErrors(errors);
          dispatch(actions.bulkDone(getSuccesses(referenceNumbers, errors), errors));
        },
        error => {
          // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
          console.error(error);
        }
      );
  };

const toggleWatchOrders = isDirectAction =>
  (actions, group, referenceNumbers, watchers) =>
    dispatch => {
      const groupMember = singularize(group);

      dispatch(actions.bulkRequest());

      const url = isDirectAction ? `/orders/${group}` : `/orders/${group}/delete`;

      Api.post(url, {
          referenceNumbers,
          [`${groupMember}Id`]: watchers[0],
        })
        .then(
          ({errors = []}) => {
            errors = parseErrors(errors);
            dispatch(actions.bulkDone(getSuccesses(referenceNumbers, errors), errors));
          },
          error => {
            // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
            console.error(error);
          }
        );
    };

export const watchOrders = toggleWatchOrders(true);
export const unwatchOrders = toggleWatchOrders(false);

const { actions, reducer } = createStore({
  entity: 'bulk',
  scope: 'orders',
  actions: {
    cancelOrders,
    changeOrdersState,
    watchOrders: toggleWatchOrders(true),
    unwatchOrders: toggleWatchOrders(false),
  },
  reducers,
});

export {
  actions,
  reducer as default
};
