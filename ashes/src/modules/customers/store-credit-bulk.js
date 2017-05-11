// libs
import _ from 'lodash';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';

// data
import { reducers } from '../bulk';

// TODO remove when https://github.com/FoxComm/phoenix-scala/issues/763 closed
const preprocessResponse = (results) => {
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

const parseChangeStateResponse = (results) => {
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

const cancelStoreCredits = (actions, ids, reasonId) =>
  dispatch => {
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

const changeStoreCreditsState = (actions, ids, state) =>
  dispatch => {
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


const { actions, reducer } = createStore({
  path: 'customers.store-credits.bulk',
  actions: {
    cancelStoreCredits,
    changeStoreCreditsState,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
