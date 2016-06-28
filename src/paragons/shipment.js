/* @flow */

export const itemStates = {
  pending: 'pending',
  delivered: 'delivered',
  cancelled: 'cancelled'
};

export const itemStateTitles = {
  [itemStates.pending]: 'Pending',
  [itemStates.delivered]: 'Delivered',
  [itemStates.cancelled]: 'Cancelled',
};
