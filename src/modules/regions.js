import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

const regionsReceived = createAction('REGIONS_RECEIVED');
const regionsFailed = createAction('REGIONS_FAILED');

export function fetchRegions() {
  return dispatch => {
    return Api.get(`/regions`)
      .then(
        regions => dispatch(regionsReceived(regions)),
        err => dispatch(regionsFailed(err))
      );
  };
}

const initialState = {};

const reducer = createReducer({
  [regionsFailed]: (state, err) => {
    console.error(err);
    return state;
  },
  [regionsReceived]: (state, regions) => {
    return _.indexBy(regions, 'id');
  }
}, initialState);

export default reducer;
