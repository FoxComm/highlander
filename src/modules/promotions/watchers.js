import makeWatchers from '../watchers';

const { actions, reducer } = makeWatchers('promotions');

export {
  actions,
  reducer as default
};
