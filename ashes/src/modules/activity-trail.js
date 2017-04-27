import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { update, assoc } from 'sprout-data';
import { createAsyncActions } from '@foxcomm/wings';

import OrderParagon from 'paragons/order';
import searchActivities from 'elastic/activities';
import types, { derivedTypes } from '../components/activity-trail/activities/base/types';

import { updateItems } from './state-helpers';

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
  return _.flatMap(activities, activity => {
    if (activity.kind == types.CART_LINE_ITEMS_UPDATED_QUANTITIES) {
      const { oldQuantities, newQuantities, ...restData } = activity.data;

      const newActivities = [];

      _.each(newQuantities, (quantity, skuName) => {
        const oldQuantity = skuName in oldQuantities ? oldQuantities[skuName] : 0;
        if (oldQuantity === quantity) return;

        const kind = oldQuantity > quantity ?
          derivedTypes.CART_LINE_ITEMS_REMOVED_SKU : derivedTypes.CART_LINE_ITEMS_ADDED_SKU;

        newActivities.push({
          ...activity,
          kind,
          data: {
            ...restData,
            skuName,
            difference: Math.abs(quantity - oldQuantity),
          }
        });
      });

      return newActivities;
    }

    return activity;
  });
}

export function mergeActivities(activities = [], newActivities) {
  const merged = updateItems(activities, newActivities, activity => {
    if (activity.kind === derivedTypes.CART_LINE_ITEMS_REMOVED_SKU ||
      activity.kind === derivedTypes.CART_LINE_ITEMS_ADDED_SKU) {
      return `${activity.id}-${activity.data.skuName}`;
    }

    return activity.id;
  });

  return _.values(merged).sort((a, b) => {
    return new Date(b.createdAt) - new Date(a.createdAt) || b.id - a.id;
  });
}

/**
 * Internal Actions
 */
const _fetchActivityTrail = createAsyncActions(
  'fetchActivityTrail',
  ({ dimension, objectId = null }, from) =>
    searchActivities(from, { dimension, objectId })
      .then(response => {
        // nginx sends empty object instead of empty array
        const result = _.isEmpty(response.result) ? [] : response.result;
        const activities = processActivities(result.map(con => {
          //TODO Using connection id as activity id until activities get
          //real ids
          let activity = con.activity;
          activity.id = con.id;
          return processActivity(activity);
        }));

        return {
          activities,
          hasMore: response.hasMore
        };
      })
);

/**
 * Exported Actions
 */
export const resetActivities = createAction('ACTIVITY_TRAIL_RESET');

export const fetchActivityTrail = _fetchActivityTrail.perform;

const initialState = {
  activities: [],
  hasMore: false,
};

const reducer = createReducer({
  [resetActivities]: () => initialState,
  [_fetchActivityTrail.succeeded]: (state, data) => {
    const updater = _.flow(
      _.partialRight(update, ['activities'], mergeActivities, data.activities),
      _.partialRight(assoc,
        ['hasMore'], data.hasMore,
        ['isFetching'], false
      )
    );

    return updater(state);
  },
}, initialState);

export default reducer;
