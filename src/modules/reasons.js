import _ from 'lodash';
import Api from '../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { ReasonType } from '../lib/reason-utils';

const reasonsRequested = createAction('REASONS_REQUESTED');
const reasonsReceived = createAction('REASONS_RECEIVED',
                                     (payload, reasonType) => [payload, reasonType]);
const reasonsFailed = createAction('REASONS_FAILED');

/**
 * loads reasons from API by type
 * @param  {ReasonType} reasonType - type of reason, defined in 'reson-utils'
 */
export function fetchReasons(reasonType) {
  return dispatch => {
    dispatch(reasonsRequested());

    return Api.get(`/reasons/${reasonType}`)
      .then(
        json => dispatch(reasonsReceived(json, reasonType)),
        err => dispatch(reasonsFailed(err))
      );
  };
}

const initialState = {};

const reducer = createReducer({
  [reasonsRequested]: state => {
    return {
      ...state,
      isFetching: true
    };
  },
  [reasonsReceived]: (state, [json, reasonType]) => {
    const data = _.get(json, 'result', json);
    return assoc(state,
      'isFetching', false,
      ['reasons', reasonType], data
    );
  },
  [reasonsFailed]: (state, err) => {
    console.error(err);
    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
