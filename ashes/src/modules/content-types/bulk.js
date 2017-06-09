/* @flow */

// libs
import _ from 'lodash';
import { flow, map, filter, reduce } from 'lodash/fp';

// helpers
import Api from '../../lib/api';
import createStore from '../../lib/store-creator';
import { getPropsByIds } from 'modules/bulk-export/helpers';

// data
import { reducers, createExportByIds } from '../bulk';

const getCodes = (getState: Function, ids: Array<number>): Object => {
  return getPropsByIds('contentTypes', ids, ['code'], getState());
};

// TODO remove when https://github.com/FoxComm/phoenix-scala/issues/763 closed
const preprocessResponse = (results: Object): Object => {
  const successes = flow(
    filter(result => result.success),
    map(result => result.code)
  )(results);

  const errors = flow(
    filter(result => !result.success),
    reduce((obj, result) => {
      return {
        ...result,
        [result.code]: result.errors[0],
      };
    }, {})
  )(results);

  return {
    batch: {
      success: {
        contentType: successes,
      },
      errors: {
        contentType: errors,
      },
    },
  };
};

const parseChangeStateResponse = (results: Object): Object => {
  const {batch} = preprocessResponse(results);

  const successes = _.reduce(batch.success.contentType,
    (result, id) => ({...result, [id]: []}), {});
  const errors = _.reduce(batch.errors.contentType,
    (result, message, id) => ({...result, [id]: [message]}), {});

  return {
    successes,
    errors,
  };
};

const cancelContentTypes = (actions: Object, codes: Array<string>, reasonId: number) =>
  (dispatch) => {
    dispatch(actions.bulkRequest());
    Api.patch('/content-types/bulk', {
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

const changeContentTypesState = (actions: Object, codes: Array<string>, state: string) =>
  dispatch => {
    dispatch(actions.bulkRequest());
    Api.patch('/content-types/bulk', {
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
  path: 'contentTypes.bulk',
  actions: {
    cancelContentTypes,
    changeContentTypesState,
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
