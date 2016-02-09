// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { get, assoc } from 'sprout-data';

// helpers
import Api from '../../lib/api';
import { haveType } from '../state-helpers';

// data
import { orderLineItemsFetchSuccess } from './line-items';
import OrderParagon from '../../paragons/order';

export const bulkRequest = createAction('BULK_REQUEST');
export const bulkSucceed = createAction('BULK_SUCCEED');
export const bulkFailed = createAction('BULK_FAILED');

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
            dispatch(bulkFailed(errors));
          } else {
            dispatch(bulkSucceed());
          }
        },
        error => {
          console.error(error);
          dispatch(bulkFailed());
        }
      );
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
  [bulkSucceed]: () => {
    return {
      isFetching: false,
    };
  },
  [bulkFailed]: (state, errors) => {
    return {
      isFetching: false,
      errors,
    };
  },
}, initialState);

export default reducer;
