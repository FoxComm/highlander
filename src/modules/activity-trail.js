
import _ from 'lodash';
import Api from '../lib/api';
import {createAction, createReducer} from 'redux-act';
import { update, assoc } from 'sprout-data';
import { updateItems } from './state-helpers';
import OrderParagon from '../paragons/order';
import searchActivities from '../elastic/activities';
import types, { derivedTypes } from '../components/activity-trail/activities/base/types';

const startFetching = createAction('ACTIVITY_TRAIL_START_FETCHING');
const receivedActivities = createAction('ACTIVITY_TRAIL_RECEIVED');
const fetchFailed = createAction('ACTIVITY_TRAIL_FETCH_FAILED');
export const resetActivities = createAction('ACTIVITY_TRAIL_RESET');

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

export function fetchActivityTrail({dimension, objectId = null}, from) {
  return dispatch => {
    dispatch(startFetching());
    searchActivities(from, {
      dimension,
      objectId
    }).then(
      response => {
        dispatch(receivedActivities(
          {
            activities: processActivities(response.result.map(({activity}) => processActivity(activity))),
            hasMore: response.hasMore
          }
        ));
      },
      err => dispatch(fetchFailed(err))
    );
  };
}

function mergeActivities(activities = [], newActivities) {
  const merged = updateItems(activities, newActivities);

  return _.values(merged).sort((a, b) => {
    return new Date(b.createdAt) - new Date(a.createdAt) || b.id - a.id;
  });
}

const initialState = {
  isFetching: null,
  err: null,
  activities: [],
  hasMore: false,
};

const reducer = createReducer({
  [startFetching]: state => {
    return assoc(state,
      ['isFetching'], true,
      ['err'], null
    );
  },
  [resetActivities]: () => {
    return initialState;
  },
  [receivedActivities]: (state, data) => {
    const updater = _.flow(
      _.partialRight(update, ['activities'], mergeActivities, data.activities),
      _.partialRight(assoc,
        ['hasMore'], data.hasMore,
        ['isFetching'], false
      )
    );

    return updater(state);
  },
  [fetchFailed]: (state, result) => {
    console.error(result);

    return assoc(state,
      ['isFetching'], false,
      ['err'], result.responseJson.error
    );
  },
}, initialState);

export default reducer;
