import makeWatchers from '../watchers';

const { actions, reducer } = makeWatchers('carts');

export {
  actions,
  reducer as default
};
