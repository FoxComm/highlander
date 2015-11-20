
import _ from 'lodash';
import Api from '../lib/api';
import { assoc, merge, get, update } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { fetchCountry } from './countries';
import { createAddress, patchAddress } from './addresses';

const DEFAULT_COUNTRY = 'US';

const changeForm = createAction('ADDRESS_FORM_CHANGE', (form, name, value) => [form, name, value]);
const assignAddress = createAction('ADDRESS_FORM_ASSIGN_ADDRESS', (form, address) => [form, address]);
const setNewCountry = createAction('ADDRESS_FORM_SET_NEW_COUNTRY', (form, country) => [form, country]);
const resetForm = createAction('ADDRESS_FORM_RESET', (form, isAdding) => [form, isAdding]);
const setError = createAction('ADDRESS_FORM_SET_ERROR', (form, err) => [form, err]);

export function changeValue(form, name, value) {
  return dispatch => {
    if (name === 'countryId') {
      dispatch(setCountry(form, value));
    }
    dispatch(changeForm(form, name, value));
  };
}

export function setCountry(form, countryId) {
  return (dispatch, getState) => {
    if (countryId == null) countryId =_.findWhere(getState().countries, {alpha2: DEFAULT_COUNTRY}).id;

    return dispatch(fetchCountry(countryId))
      .then(country => {
        dispatch(setNewCountry(form, country));
      });
  };
}

export function init(form, address, addressType) {
  return dispatch => {
    if (address) {
      dispatch(assignAddress(form, address, addressType));
    } else {
      dispatch(resetForm(form, true));
      dispatch(setCountry(form));
    }
  };
}


export function submitForm(form, customerId, formData) {
  return (dispatch, getState) => {
    const state = get(getState(), ['addressForm', form]);

    if (state.isAdding) {
      return dispatch(createAddress(customerId, formData))
        .then(address => dispatch(setError(form, null)) && address)
        .catch(err => dispatch(setError(form, err)) && err);
    } else {
      return dispatch(patchAddress(customerId, state.addressId, formData))
        .then(address => dispatch(setError(form, null)) && address)
        .catch(err => dispatch(setError(form, err)) && err);
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [changeForm]: (state, [form, name, value]) => {
    const path = name === 'countryId' ? [form, name] : [form, 'formData', name];

    return assoc(state, path, value);
  },
  [assignAddress]: (state, [form, address, addressType]) => {
    const {region, ...formData} = address || {};

    formData.regionId = region && region.id;
    const countryId = region && region.countryId;

    return assoc(state,
      [form, 'isAdding'], false,
      [form, 'formData'], formData,
      [form, 'countryId'], countryId,
      [form, 'addressId'], address.id,
      [form, 'addressType'], addressType
    );
  },
  [setNewCountry]: (state, [form, country]) => {
    return assoc(state,
      [form, 'countryId'], country.id,
      [form, 'formData', 'regionId'], country.regions[0].id
    );
  },
  [resetForm]: (state, [form, isAdding]) => {
    return assoc(state,
      [form, 'isAdding'], isAdding,
      [form, 'formData'], {}
    );
  },
  [setError]: (state, [form, err]) => {
    if (err) console.error(err);

    return assoc(state,
      [form, 'err'], err
    );
  }
}, initialState);

export default reducer;
