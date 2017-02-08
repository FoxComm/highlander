// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import Api from '../../lib/api';
import { singularize } from 'fleck';
import createStore from '../../lib/store-creator';

// data
import { initialState, reducers } from '../bulk';

// TODO remove when https://github.com/FoxComm/phoenix-scala/issues/763 closed
const preprocessResponse = (results) => {
  const successes = results
    .filter(({success}) => success)
    .map(({code}) => code);

  const errors = results
    .filter(({success}) => !success)
    .reduce((result, {code, errors}) => ({...result, [code]: errors[0]}), {});

  return {
    batch: {
      success: {
        giftCard: successes,
      },
      errors: {
        giftCard: errors,
      },
    },
  };
};

const parseChangeStateResponse = (results) => {
  const {batch} = preprocessResponse(results);

  const successes = _.reduce(batch.success.giftCard,
    (result, id) => ({...result, [id]: []}), {});
  const errors = _.reduce(batch.errors.giftCard,
    (result, message, id) => ({...result, [id]: [message]}), {});

  return {
    successes,
    errors,
  };
};

const cancelGiftCards = (actions, codes, reasonId) =>
  dispatch => {
    dispatch(actions.bulkRequest());
    Api.patch('/gift-cards/bulk', {
        codes,
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

const changeGiftCardsState = (actions, codes, state) =>
  dispatch => {
    dispatch(actions.bulkRequest());
    Api.patch('/gift-cards/bulk', {
        codes,
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
  path: 'giftCards.bulk',
  actions: {
    cancelGiftCards,
    changeGiftCardsState,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
