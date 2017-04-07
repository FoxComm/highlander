// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import Api from '../../lib/api';
import { singularize } from 'fleck';
import createStore from '../../lib/store-creator';

// data
import { initialState, reducers, getSuccesses as _getSuccesses, bulkActions } from '../bulk';

const getSuccesses = _.partial(_getSuccesses, 'order');

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
  },
  reducers,
});

export {
  actions,
  reducer as default
};
