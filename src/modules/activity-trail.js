
import _ from 'lodash';
import Api from '../lib/api';
import {createAction, createReducer} from 'redux-act';
import { update, assoc } from 'sprout-data';
import { updateItems } from './state-helpers';
import OrderParagon from '../paragons/order';
import searchActivities from '../elastic/activities';

const startFetching = createAction('ACTIVITY_TRAIL_START_FETCHING');
const receivedActivities = createAction('ACTIVITY_TRAIL_RECEIVED', (trailId, data) => [trailId, data]);
const setError = createAction('ACTIVITY_TRAIL_FAILED');

export function fetchActivityTrail(entity, from) {
  return dispatch => {
    dispatch(startFetching(entity.entityId));
    searchActivities(from).then(
      response => {
        dispatch(receivedActivities(
          entity.entityId,
          {
            activities: response.result.map(({activity}) => {
              if (activity.data.order) {
                activity.data.order = new OrderParagon(activity.data.order);
              }
              return activity;
            }),
            hasMore: response.hasMore
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
  [startFetching]: (state, trailId) => {
    return assoc(state, [trailId, 'isFetching'], true);
  },
  [receivedActivities]: (state, [trailId, data]) => {
    const updater = _.flow(
      _.partialRight(update, [trailId, 'activities'], mergeActivities, data.activities),
      _.partialRight(assoc,
        [trailId, 'hasMore'], data.hasMore,
        [trailId, 'isFetching'], false
      )
    );

    return updater(state);
  },
  [setError]: (state, err) => {
    console.error(err);
    return {
      ...state,
      err
    };
  }
}, initialState);

export default reducer;
