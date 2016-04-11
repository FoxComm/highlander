// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import Api from '../../lib/api';
import { singularize } from 'fleck';
import createStore from '../../lib/store-creator';

// data
import { initialState, reducers } from '../bulk';

const getSuccesses = (entityIds, bulkStatus) => {
  const orderFailures = _.get(bulkStatus, 'failures.order', {});

  return entityIds
    .filter(referenceNumber => !(referenceNumber in orderFailures))
    .reduce((result, referenceNumber) => {
      return {
        ...result,
        [referenceNumber]: []
      };
    }, {});
};

const cancelOrders = (actions, entityIds, reasonId) =>
  dispatch => {
    dispatch(actions.bulkRequest());
    Api.patch('/orders', {
        entityIds,
        reasonId,
        state: 'canceled',
      })
      .then(
        ({batch}) => {
          const errors = _.get(batch, 'failures.order');
          dispatch(actions.bulkDone(getSuccesses(entityIds, batch), errors));
        },
        error => {
          dispatch(actions.bulkError(error));
        }
      );
  };

const changeOrdersState = (actions, entityIds, state) =>
  dispatch => {
    dispatch(actions.bulkRequest());
    Api.patch('/orders', {
        entityIds,
        state,
      })
      .then(
        ({batch}) => {
          const errors = _.get(batch, 'failures.order');
          dispatch(actions.bulkDone(getSuccesses(entityIds, batch), errors));
        },
        error => {
          dispatch(actions.bulkError(error));
        }
      );
  };

const toggleWatchOrders = isDirectAction =>
  (actions, group, entityIds, watchers) =>
    dispatch => {
      dispatch(actions.bulkRequest());

      const url = isDirectAction ? `/orders/${group}` : `/orders/${group}/delete`;
      const storeAdminId = watchers[0];

      Api.post(url, {
          entityIds,
          storeAdminId,
        })
        .then(
          ({batch}) => {
            const errors = _.get(batch, 'failures.order');
            dispatch(actions.bulkDone(getSuccesses(entityIds, batch), errors));
          },
          error => {
            dispatch(actions.bulkError(error));
          }
        );
    };


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
