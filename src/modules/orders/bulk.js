// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import Api from '../../lib/api';
import { singularize } from 'fleck';
import createStore from '../../lib/store-creator';

// data
import { initialState, reducers } from '../bulk';

const getSuccesses = (referenceNumbers, bulkStatus) => {
  const orderFailures = _.get(bulkStatus, 'failures.order');

  return referenceNumbers
    .filter(referenceNumber => !(referenceNumber in orderFailures))
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
        ({batch}) => {
          const errors = _.get(batch, 'failures.order');

          dispatch(actions.setMessages({
            success: 'successfully canceled',
            error: 'could not be canceled',
          }));
          dispatch(actions.bulkDone(getSuccesses(referenceNumbers, batch), errors));
        },
        error => {
          dispatch(actions.bulkError(error));
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
        ({batch}) => {
          const errors = _.get(batch, 'failures.order');

          dispatch(actions.setMessages({
            success: 'successfully changed',
            error: 'could not be changed',
          }));
          dispatch(actions.bulkDone(getSuccesses(referenceNumbers, batch), errors));
        },
        error => {
          dispatch(actions.bulkError(error));
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
          ({batch}) => {
            const errors = _.get(batch, 'failures.order');

            dispatch(actions.setMessages({
              success: 'successfully watched',
              error: 'could not be watched',
            }));
            dispatch(actions.bulkDone(getSuccesses(referenceNumbers, batch), errors));
          },
          error => {
            dispatch(actions.bulkError(error));
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
