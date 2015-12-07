
import _ from 'lodash';
import Api from '../lib/api';
import {createAction, createReducer} from 'redux-act';
import {update} from 'sprout-data';
import { updateItems } from './state-helpers';

const receivedActivities = createAction('ACTIVITY_TRAIL_RECEIVED', (trailId, activities) => [trailId, activities]);

export function fetchActivityTrail(entity, from) {
  return dispatch => {
    const trail = require('./fixtures/activity-trail.json');

    dispatch(receivedActivities(entity.entityId, trail));
  };
}

function mergeActivities(activities, newActivities) {
  const merged = updateItems(activities, newActivities);

  return _.values(merged).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
}

const initialState = {};

const reducer = createReducer({
  [receivedActivities]: (state, [trailId, newActivities]) => {
    return update(state, [trailId, 'activities'], mergeActivities, newActivities);
  }
}, initialState);

export default reducer;
