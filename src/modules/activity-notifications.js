
import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { deepMerge } from 'sprout-data';

const notificationReceived = createAction('NOTIFICATION_RECEIVED');
export const toggleNotifications = createAction('NOTIFICATIONS_TOGGLE');

export function startFetchingNotifications() {
  console.log('starting fetching');
  return (dispatch) => {
    const eventSource = new EventSource('/sse/v1/notifications/1', {withCredentials: true});

    eventSource.onmessage = function(e) {
      console.log(e);
      if (_.isEmpty(e.data)) {
        console.log('heartbeat');
      } else {
        console.log('Received data');
        console.log(e.data);
        const data = JSON.parse(e.data);
        dispatch(notificationReceived(data));
      }
    };

    eventSource.onopen = function(e) {
      console.log('Connection was opened.');
    };

    eventSource.onerror = function(e) {
      console.log('Connection was closed.');
    };
  };
}

export function markAsReadAndClose() {
  console.log('mark all as read');
  return dispatch => {
    dispatch(toggleNotifications());
  }
}

const initialState = {
  displayed: false
};

const reducer = createReducer({
  [notificationReceived]: (state, data) => {
    const notificationList = _.get(state, 'notifications', []);
    const updatedNotifications = notificationList.concat([data]);

    return {
      ...state,
      notifications: updatedNotifications,
      count: updatedNotifications.length
    };
  },
  [toggleNotifications]: state => {
    const displayed = state.displayed;
    return {
      ...state,
      displayed: !displayed
    };
  }
}, initialState);

export default reducer;
