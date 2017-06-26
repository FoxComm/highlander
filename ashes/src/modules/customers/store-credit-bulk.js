/* @flow */

// libs
import _ from 'lodash';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';
import { getPropsByIds } from 'modules/bulk-export/helpers';

// data
import { reducers, createExportByIds } from '../bulk';

// TODO remove when https://github.com/FoxComm/phoenix-scala/issues/763 closed
const preprocessResponse = (results: Object): Object => {
  const successes = results
    .filter(({success}) => success)
    .map(({id}) => id);

  const errors = results
    .filter(({success}) => !success)
    .reduce((result, {id, errors}) => ({...result, [id]: errors[0]}), {});

  return {
    batch: {
      success: {
        storeCredit: successes,
      },
      errors: {
        storeCredit: errors,
      },
    },
  };
};

const parseChangeStateResponse = (results: Object): Object => {
  const {batch} = preprocessResponse(results);

  const successes = _.reduce(batch.success.storeCredit,
    (result, id) => ({...result, [id]: []}), {});
  const errors = _.reduce(batch.errors.storeCredit,
    (result, message, id) => ({...result, [id]: [message]}), {});

  return {
    successes,
    errors,
  };
};

const cancelStoreCredits = (actions: Object, ids: Array<number>, reasonId: number) =>
  (dispatch: Function) => {
    dispatch(actions.bulkRequest());
    Api.patch('/store-credits', {
      ids,
      reasonId,
      state: 'canceled',
    })
      .then(
        (result) => {
          const {successes, errors} = parseChangeStateResponse(result);
          dispatch(actions.bulkDone(successes, errors));
        },
        error => {
          // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
          console.error(error);
        }
      );
  };

const changeStoreCreditsState = (actions: Object, ids: Array<number>, state: string) =>
  (dispatch: Function) => {
    dispatch(actions.bulkRequest());
    Api.patch('/store-credits', {
      ids,
      state,
    })
      .then(
        (result) => {
          const {successes, errors} = parseChangeStateResponse(result);
          dispatch(actions.bulkDone(successes, errors));
        },
        error => {
          // TODO handle when https://github.com/FoxComm/Ashes/issues/466 closed
          console.error(error);
        }
      );
  };

const getSC = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('customers', ids, ['id'], getState(), 'storeCredits');
};

const exportByIds = createExportByIds(getSC);

const { actions, reducer } = createStore({
  path: 'customers.store-credits.bulk',
  actions: {
    cancelStoreCredits,
    changeStoreCreditsState,
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
