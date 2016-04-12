// libs
import _ from 'lodash';

// data
import makeWatchers from '../watchers';

const { actions, reducer } = makeWatchers('orders');

export {
  actions,
  reducer as default
};
