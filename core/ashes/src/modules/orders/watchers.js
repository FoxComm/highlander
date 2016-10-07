import makeWatchers from '../watchers';

const { actions, reducer } = makeWatchers('orders');

export {
  actions,
  reducer as default
};
