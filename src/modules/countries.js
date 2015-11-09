
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';

const countryReceived = createAction('COUNTRY_RECEIVED');
const countryFailed = createAction('COUNTRY_FAILED', (id, err) => [id, err]);

export function fetchCountry(countryId) {
  return dispatch => {
    Api.get(`/countries/{countryId}`)
      .then(json => dispatch(countryReceived(json)))
      .catch(err => dispatch(countryFailed(id, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [countryReceived]: (state, country) => {
    return {
      ...state,
      [country.id]: country
    };
  },
  [countryFailed]: (state, [id, err]) => {
    console.error(err);
  }
}, initialState);

export default reducer;
