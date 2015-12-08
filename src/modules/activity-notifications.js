
import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { deepMerge } from 'sprout-data';

const notificationsReceived = createAction('NOTIFICATIONS_RECEIVED');
export const toggleNotifiactions = createAction('NOTIFICATIONS_TOGGLE');

export function fetchNotifications() {
  return (dispatch) => {
    dispatch(notificationsReceived(data));
  };
}

const initialState = {
  displayed: false
};

const data = [
  {
    'id': 1,
    'body': {
      'action': 'assigned',
      'origin': {
        'id': 1,
        'name': 'Frankly Admin',
        'email': 'admin@admin.com'
      },
      'reference': {
        'ref': 'BR10001',
        'url': 'orders/BR10001',
        'typed': 'Order'
      }
    },
    'createdAt': '2015-12-03T20:22:23.172Z',
    'isRead': false
  },
  {
    'id': 2,
    'body': {
      'action': 'unassigned',
      'origin': {
        'id': 2,
        'name': 'Such Root',
        'email': 'hackerman@yahoo.com'
      },
      'reference': {
        'ref': 'BR10002',
        'url': 'orders/BR10002',
        'typed': 'Order'
      }
    },
    'createdAt': '2015-12-03T20:22:23.173Z',
    'isRead': false
  },
  {
    'id': 3,
    'body': {
      'action': 'marked as Complete',
      'origin': {
        'id': 3,
        'name': 'Admin Hero',
        'email': 'admin_hero@xakep.ru'
      },
      'reference': {
        'ref': 'BR10001',
        'url': 'orders/BR10001',
        'typed': 'Order'
      }
    },
    'createdAt': '2015-12-03T20:22:23.173Z',
    'isRead': false
  },
  {
    'id': 4,
    'body': {
      'action': 'edited shipping address',
      'origin': {
        'id': 2,
        'name': 'Such Root',
        'email': 'hackerman@yahoo.com'
      },
      'reference': {
        'ref': 'BR10003',
        'url': 'orders/BR10003',
        'typed': 'Order'
      }
    },
    'createdAt': '2015-12-03T20:22:23.173Z',
    'isRead': false
  },
  {
    'id': 5,
    'body': {
      'action': 'added a note',
      'origin': {
        'id': 3,
        'name': 'Admin Hero',
        'email': 'admin_hero@xakep.ru'
      },
      'reference': {
        'ref': 'BR10001',
        'url': 'orders/BR10001',
        'typed': 'Order'
      }
    },
    'createdAt': '2015-12-03T19:52:23.176Z',
    'isRead': true
  },
  {
    'id': 6,
    'body': {
      'action': 'changed state to FulfillmentStarted',
      'origin': {},
      'reference': {
        'ref': 'BR10003',
        'url': 'orders/BR10003',
        'typed': 'Order'
      }
    },
    'createdAt': '2015-12-03T19:37:23.178Z',
    'isRead': true
  }
];

const reducer = createReducer({
  [notificationsReceived]: (state, data) => {
    return {
      ...state,
      notifications: data,
      count: data.length
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
