"use strict";

import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { deepMerge } from 'sprout-data';

const countryReceived = createAction('COUNTRY_RECEIVED');
const countryFailed = createAction('COUNTRY_FAILED', (id, err) => [id, err]);

const countriesReceived = createAction('COUNTRIES_RECEIVED');
const countriesFailed = createAction('COUNTRIES_FAILED');


export function fetchCountry(countryId) {
  return dispatch => {
    return Api.get(`/countries/${countryId}`)
      .then(json => dispatch(countryReceived(json)) && json)
      .catch(err => dispatch(countryFailed(id, err)) && err);
  };
}

export function fetchCountries() {
  return dispatch => {
    return Api.get('/countries')
      .then(json => dispatch(countriesReceived(json)))
      .catch(err => dispatch(countriesFailed(err)));
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
  [countriesReceived]: (state, countries) => {
    return deepMerge(state, _.indexBy(countries, 'id'));
  },
  [countriesFailed]: (state, err) => {
    console.error(err);
    return state;
  },
  [countryFailed]: (state, [id, err]) => {
    console.error(err);
    return state;
  }
}, initialState);

export default reducer;
