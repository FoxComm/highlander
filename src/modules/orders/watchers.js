// libs
import _ from 'lodash';

// data
import { fetchOrder } from './details';
import makeWatchers from '../watchers';

const { actions, reducer } = makeWatchers('orders', {fetchEntity: fetchOrder});

export {
  actions,
  reducer as default
};
