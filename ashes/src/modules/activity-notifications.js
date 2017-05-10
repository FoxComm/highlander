import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';

const notificationReceived = createAction('NOTIFICATION_RECEIVED');
const markNotificationsAsRead = createAction('NOTIFICATIONS_MARK_AS_READ');
export const toggleNotifications = createAction('NOTIFICATIONS_TOGGLE');

export function startFetchingNotifications() {
  return (dispatch) => {
    if (typeof EventSource != 'undefined') {
      const eventSource = new EventSource(`/sse/v1/notifications`, { withCredentials: true });

      eventSource.onmessage = function (e) {
        if (!_.isEmpty(e.data)) {
          const data = JSON.parse(e.data);
          dispatch(notificationReceived(data));
        }
      };

      eventSource.onopen = function (e) {
        console.info('Connection was opened.');
      };

      eventSource.onerror = function (e) {
        console.info('Connection was closed.');
      };
    }
  };
}

export function markAsReadAndClose() {
  return (dispatch, getState) => {

    dispatch(toggleNotifications());

    const activities = _.get(getState(), ['activityNotifications', 'notifications'], []);
    const activityId = _.get(_.head(activities), 'id');

    if (!_.isEmpty(activities) && _.isNumber(activityId)) {
      Api.post(`/notifications/last-seen/${activityId}`, {}).then(
        () => {
          dispatch(markNotificationsAsRead());
        }
      );
    }
  };
}

export function markAsRead() {
  return (dispatch, getState) => {

    const activities = _.get(getState(), ['activityNotifications', 'notifications'], []);

    if (!_.isEmpty(activities)) {
      const activityId = _.get(_.last(activities), 'id');

      if (_.isNumber(activityId)) {
        Api.post(`/notifications/last-seen/${activityId}`, {}).then(
          () => {
            dispatch(markNotificationsAsRead());
            dispatch(toggleNotifications());
          }
        );
      }
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
    const updatedNotifications = [notReadData, ...notificationList];
    const uniqueNotifications = _.uniq(updatedNotifications, 'id'); // fix duplicate notifications from server

    const newCount = uniqueNotifications.reduce((acc, item) => {
      if (!item.isRead) {
        acc++;
      }
      return acc;
    }, 0);

    return {
      ...state,
      notifications: uniqueNotifications,
      count: newCount
    };
  },
  [markNotificationsAsRead]: state => {
    const notificationList = _.get(state, 'notifications', []);
    const readNotifications = notificationList.map((item) => {
      return assoc(item, 'isRead', true);
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
