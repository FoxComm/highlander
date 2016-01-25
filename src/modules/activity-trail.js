
import _ from 'lodash';
import Api from '../lib/api';
import {createAction, createReducer} from 'redux-act';
import { update, assoc } from 'sprout-data';
import { updateItems } from './state-helpers';
import OrderParagon from '../paragons/order';
import searchActivities from '../elastic/activities';
import types, { derivedTypes } from '../components/activity-trail/activities/base/types';

const startFetching = createAction('ACTIVITY_TRAIL_START_FETCHING');
const receivedActivities = createAction('ACTIVITY_TRAIL_RECEIVED', (trailId, data) => [trailId, data]);
const fetchFailed = createAction('ACTIVITY_TRAIL_FETCH_FAILED', (trailId, err) => [trailId, err]);

const flatMap = _.compose(_.flatten, _.map);

export function processActivity(activity) {
  if (activity.data.order) {
    activity.data.order = new OrderParagon(activity.data.order);
  }
  if (activity.data.orderRefNum) {
    activity.data.order = new OrderParagon({
      referenceNumber: activity.data.orderRefNum,
      orderState: activity.data.orderState,
    });
  }
  return activity;
}

export function processActivities(activities) {
  return flatMap(activities, activity => {
    if (activity.kind == types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES ||
      activity.kind == types.ORDER_LINE_ITEMS_UPDATED_QUANTITIES_BY_CUSTOMER) {
      const { oldQuantities, newQuantities, ...restData } = activity.data;

      let newActivities = [];

      _.each(newQuantities, (quantity, skuName) => {
        const oldQuantity = skuName in oldQuantities ? oldQuantities[skuName] : 0;
        const kind = oldQuantity > quantity ?
          derivedTypes.ORDER_LINE_ITEMS_REMOVED_SKU : derivedTypes.ORDER_LINE_ITEMS_ADDED_SKU;

        newActivities = [...newActivities, {
          ...activity,
          kind,
          data: {
            ...restData,
            skuName,
            difference: Math.abs(quantity - oldQuantity),
          }
        }];
      });

      _.each(oldQuantities, (quantity, skuName) => {
        if (skuName in newQuantities) return;

        newActivities = [...newActivities, {
          ...activity,
          kind: derivedTypes.ORDER_LINE_ITEMS_REMOVED_SKU,
          data: {
            ...restData,
            skuName,
            difference: quantity,
          }
        }];
      });

      return newActivities;
    }

    return activity;
  });
}

export function fetchActivityTrail(entity, from) {
  return dispatch => {
    dispatch(startFetching(entity.entityId));
    searchActivities(from).then(
      response => {
        dispatch(receivedActivities(
          entity.entityId,
          {
            activities: processActivities(response.result.map(({activity}) => processActivity(activity))),
            hasMore: response.hasMore
          }
        ));
      },
      err => dispatch(fetchFailed(entity.entityId, err))
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
    return assoc(state,
      [trailId, 'isFetching'], true,
      [trailId, 'err'], null
    );
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
  [fetchFailed]: (state, [trailId, result]) => {
    console.error(result);

    return assoc(state,
      [trailId, 'isFetching'], false,
      [trailId, 'err'], result.responseJson.error
    );
  }
}, initialState);

export default reducer;
