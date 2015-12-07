
import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { deepMerge } from 'sprout-data';

const notificationsReceived = createAction('NOTIFICATIONS_RECEIVED');
export const toggleNotifiactions = createAction('NOTIFICATIONS_TOGGLE');

export function fetchNotifications() {
  return (dispatch) => {
    dispatch(notificationsReceived());
  };
}

const initialState = {
  displayed: false
};

const reducer = createReducer({
  [notificationsReceived]: state => {
    return {
      ...state,
      count: 5
    };
  },
  [toggleNotifiactions]: state => {
    const displayed = state.displayed;
    return {
      ...state,
      displayed: !displayed
    };
  }
}, initialState);

export default reducer;
