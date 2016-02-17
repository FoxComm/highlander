// libs
import _ from 'lodash';

// data
import { fetchOrder } from './details';
import * as watchers from '../watchers';
import { entityForms } from '../../paragons/watcher';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';
import { getSingularForm, getPluralForm } from '../../lib/text-utils';


const store = createStore(
  'orderWatchers',
  {
    ...watchers.actions,
    addWatchers: (referenceNumber, actions) => {
      return (dispatch, getState) => {
        const {orders: {watchers}} = getState();

        const group = _.get(watchers, 'selectModal.group');
        const items = _.get(watchers, 'selectModal.selected', []);

        const data = {
          [group]: items.map((item) => item.id)
        };

        Api.post(`/orders/${referenceNumber}/${group}`, data).then(
          () => {
            dispatch(actions.clearSelected());
            dispatch(actions.hideSelectModal());
            dispatch(fetchOrder(referenceNumber));
          },
          (error) => dispatch(actions.failWatchersAction(error))
        );
      };
    },
    removeWatcher: (referenceNumber, group, id, actions) => {
      return (dispatch, getState) => {
        const {orders: {watchers}} = getState();

        Api.delete(`/orders/${referenceNumber}/${group}/${id}`).then(
          () => dispatch(fetchOrder(referenceNumber)),
          (error) => dispatch(actions.failWatchersAction(error))
        );
      };
    },
    //TODO will be expanded to many-to-many scenario with https://github.com/FoxComm/phoenix-scala/issues/777
  },
  watchers.creators,
  watchers.initialState
);

export const actions = store.actions;
export default store.reducer;
