
import _ from 'lodash';
import Api from '../lib/api';
import {createAction, createReducer} from 'redux-act';
import { update, assoc } from 'sprout-data';
import { updateItems } from './state-helpers';
import OrderParagon from '../paragons/order';
import { fetch } from '../es/activities';

const receivedActivities = createAction('ACTIVITY_TRAIL_RECEIVED', (trailId, data) => [trailId, data]);
const setError = createAction('ACTIVITY_TRAIL_FAILED');

export function fetchActivityTrail(entity, from) {
  return dispatch => {
    fetch(from).then(
      result => {
        dispatch(receivedActivities(
          entity.entityId,
          {
            activities: result.activities.map(activity => {
              if (activity.data.order) {
                activity.data.order = new OrderParagon(activity.data.order);
              }
              return activity;
            }),
            hasMore: result.hasMore
          }
        ));
      },
      err => setError(err)
    );
  };
}

function mergeActivities(activities = [], newActivities) {
  const merged = updateItems(activities, newActivities);

  return _.values(merged).sort((a, b) => {
    return new Date(b.createdAt) - new Date(a.createdAt) || b.id - a.id;
  });
}

const initialState = {};

const reducer = createReducer({
  [receivedActivities]: (state, [trailId, data]) => {
    const updater = _.flow(
      _.partialRight(update, [trailId, 'activities'], mergeActivities, data.activities),
      _.partialRight(assoc, [trailId, 'hasMore'], data.hasMore)
    );

    return updater(state);
  },
  [setError]: (state, err) => {
    return {
      ...state,
      err
    };
  }
}, initialState);

export default reducer;
