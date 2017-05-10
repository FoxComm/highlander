// libs
import _ from 'lodash';
import { flow, filter, getOr, invoke, map } from 'lodash/fp';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';

// data
import { initialState, reducers, getSuccesses as _getSuccesses, bulkActions, createExportByIds } from '../bulk';

const getSuccesses = _.partial(_getSuccesses, 'order');

const getOrders = (getState, ids) => {
  const orders =  flow(
    invoke('orders.list.currentSearch'),
    getOr([], 'results.rows'),
    filter(o => ids.indexOf(o.id) !== -1),
    map(order => order.referenceNumber)
  )(getState());
  return getSuccesses(orders);
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
          dispatch(actions.bulkDone(getSuccesses(referenceNumbers, batch), errors));
        },
        error => {
          dispatch(actions.bulkError(error));
        }
      );
  };

const exportByIds = createExportByIds(getOrders);

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
          dispatch(actions.bulkDone(getSuccesses(referenceNumbers, batch), errors));
        },
        error => {
          dispatch(actions.bulkError(error));
        }
      );
  };

const { actions, reducer } = createStore({
  path: 'orders.bulk',
  actions: {
    cancelOrders,
    changeOrdersState,
    ...bulkActions,
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
