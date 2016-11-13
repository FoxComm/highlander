
import { createReducer } from 'redux-act';
import createAsyncActions from './async-utils';
import { assoc } from 'sprout-data';

function apiFetchCountries() {
  return this.api.get('/v1/public/countries');
}

function apiFetchCountry(countryId) {
  return this.api.get(`/v1/public/countries/${countryId}`);
}

const countriesActions = createAsyncActions('countries', apiFetchCountries);
const countryActions = createAsyncActions('country', apiFetchCountry);

export const fetchCountry = countryActions.fetch;

export const usaDetails = require('./mock/usa-details.json');

export function loadCountry(id) {
  return (dispatch, getState) => {
    const countryDetails = getState().countries.details;
    if (!countryDetails[id]) {
      return dispatch(fetchCountry(id));
    }
    return Promise.resolve(countryDetails[id]);
  };
}

const initialState = {
  list: [
    {id: 234, name: 'UNITED STATES', alpha3: 'USA'},
    {id: 39, name: 'CANADA', alpha3: 'CAN'},
  ],
  details: {
    234: usaDetails,
    39: require('./mock/can-details.json'),
  },
};

const reducer = createReducer({
  [countriesActions.succeeded]: (state, result) => {
    return assoc(state,
      'list', result
    );
  },
  [countryActions.succeeded]: (state, result) => {
    return assoc(state,
      ['details', result.id], result
    );
  },
}, initialState);

export default reducer;
