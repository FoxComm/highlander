// libs
import _ from 'lodash';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';

// data
import { initialState, reducers, getSuccesses as _getSuccesses, bulkActions } from '../bulk';
import { bulkExportByIds } from 'modules/bulk-export/bulk-export';

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

const exportByIds = (actions, refNums, ids, description, fields, entity, identifier) => (dispatch) => {
  dispatch(actions.bulkRequest());

  dispatch(bulkExportByIds(ids, description, fields, entity, identifier))
    .then(() => dispatch(actions.bulkDone(getSuccesses(refNums), null)))
    .catch(err => dispatch(actions.bulkError(err)));
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
    exportByIds
  },
  reducers,
});

export {
  actions,
  reducer as default
};
