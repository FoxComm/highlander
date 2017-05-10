/* @flow */

// libs
import _ from 'lodash';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';
import { getPropsByIds } from 'modules/bulk-export/helpers';

// data
import { reducers, createExportByIds } from '../bulk';
const getCodes = (getState: Function, ids: Array<number>): Object => getPropsByIds('giftCards', ids, ['code'], getState());

// TODO remove when https://github.com/FoxComm/phoenix-scala/issues/763 closed
const preprocessResponse = (results: Object): Object => {
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

const parseChangeStateResponse = (results: Object): Object => {
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

const cancelGiftCards = (actions: Object, codes: Array<string>, reasonId: number) =>
  (dispatch) => {
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

const changeGiftCardsState = (actions: Object, codes: Array<string>, state: string) =>
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

const exportByIds = createExportByIds(getCodes);

const { actions, reducer } = createStore({
  path: 'giftCards.bulk',
  actions: {
    cancelGiftCards,
    changeGiftCardsState,
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
