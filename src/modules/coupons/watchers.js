import makeWatchers from '../watchers';

const { actions, reducer } = makeWatchers('coupons');

export {
  actions,
  reducer as default
};
