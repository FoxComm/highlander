
import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, deepMerge } from 'sprout-data';

const notificationReceived = createAction('NOTIFICATION_RECEIVED');
const markNotificationsAsRead = createAction('NOTIFICATIONS_MARK_AS_READ');
export const toggleNotifications = createAction('NOTIFICATIONS_TOGGLE');

export function startFetchingNotifications() {
  return (dispatch) => {
    const eventSource = new EventSource('/sse/v1/public/notifications/1', {withCredentials: true});

    eventSource.onmessage = function(e) {
      if (!_.isEmpty(e.data)) {
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
    const activityId = _.get(_.last(activities), 'id');
    const dispatchToggle = () => dispatch(toggleNotifications());

    let willFinished = Promise.resolve();

    if (!_.isEmpty(activities) && _.isNumber(activityId)) {
      willFinished = Api.post(`/public/notifications/${adminId}/last-seen/${activityId}`, {}).then(
        () => {
          dispatch(markNotificationsAsRead());
        }
      );
    }

    willFinished.then(dispatchToggle, dispatchToggle);
  };
}

export function markAsRead() {
  return (dispatch, getState) => {
    const adminId = 1;
    const activities = _.get(getState(), ['activityNotifications', 'notifications'], []);

    if (!_.isEmpty(activities)) {
      const activityId = _.get(_.head(activities), 'id');

      if (_.isNumber(activityId)) {
        Api.post(`/public/notifications/${adminId}/last-seen/${activityId}`, {}).then(
          () => dispatch(markNotificationsAsRead()),
          () => dispatch(toggleNotifications())
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
