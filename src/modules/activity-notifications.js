
import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { deepMerge } from 'sprout-data';

const notificationsReceived = createAction('NOTIFICATIONS_RECEIVED');

export function fetchNotifications() {
  return (dispatch) => {
    dispatch(notificationsReceived());
  };
}

const initialState = {};

const reducer = createReducer({
  [notificationsReceived]: state => {
    return {
      count: 5
    };
  }
}, initialState);

export default reducer;
