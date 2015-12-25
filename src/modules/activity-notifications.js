
import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, deepMerge } from 'sprout-data';

const notificationReceived = createAction('NOTIFICATION_RECEIVED');
const markNotificationsAsRead = createAction('NOTIFICATIONS_MARK_AS_READ');
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
  return (dispatch, getState) => {
    const adminId = 1;
    const activities = _.get(getState(), ['activityNotifications', 'notifications'], []);

    if (!_.isEmpty(activities)) {
      const activityId = _.get(_.last(activities), 'id');

      Api.post(`/notifications/${adminId}/last-seen/${activityId}`, {}).then(
        () => dispatch(markNotificationsAsRead()),
        () => dispatch(toggleNotifications())
      );
    } else {
      dispatch(toggleNotifications());
    }
  };
}

export function markAsRead() {
  return (dispatch, getState) => {
    const adminId = 1;
    const activities = _.get(getState(), ['activityNotifications', 'notifications'], []);

    if (!_.isEmpty(activities)) {
      const activityId = _.get(_.last(activities), 'id');

      Api.post(`/notifications/${adminId}/last-seen/${activityId}`, {}).then(
        () => dispatch(markNotificationsAsRead()),
        () => dispatch(toggleNotifications())
      );
    } else {
      dispatch(toggleNotifications());
    }
  };
}

const initialState = {
  displayed: false,
  notifications: [],
  count: 0
};

const reducer = createReducer({
  [notificationReceived]: (state, data) => {
    const notificationList = _.get(state, 'notifications', []);
    const notReadData = assoc(data, 'isRead', false);
    const updatedNotifications = notificationList.concat([notReadData]);
    const newCount = updatedNotifications.reduce((acc, item) => {
      if (!item.isRead) {
        acc++;
      }
      return acc;
    }, 0);

    return {
      ...state,
      notifications: updatedNotifications,
      count: newCount
    };
  },
  [markNotificationsAsRead]: state => {
    const notificationList = _.get(state, 'notifications', []);
    const readNotifications = notificationList.map((item) => {
      const copy = item;
      copy.isRead = true;
      return copy;
    });

    return {
      ...state,
      notifications: readNotifications,
      count: 0
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
