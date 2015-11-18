
import _ from 'lodash';
import Api from '../lib/api';
import { assoc, merge, get } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { fetchCountry } from './countries';
import { createAddress, patchAddress } from './customers/addresses';

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

export function init(form, address) {
  return dispatch => {
    if (address) {
      dispatch(assignAddress(form, address));
    } else {
      dispatch(resetForm(form, true));
      dispatch(setCountry(form));
    }
  };
}

/**
 * Prepare value before submitting to server
 * @param name
 * @param value
 */
function prepareValue(name, value) {
  switch (name) {
    case 'phoneNumber':
      return value.replace(/[^\d]/g, '');
      break;
    default:
      return value;
  }
}

export function submitForm(form, customerId) {
  return (dispatch, getState) => {
    const state = get(getState(), ['addressForm', form]);

    const formData = _.transform(state.formData, (result, value, name) => {
      result[name] = prepareValue(name, value);
    });

    if (state.isAdding) {
      return Api.post(`/customers/${customerId}/addresses`, formData)
        .then(address => dispatch(setError(null)) && address)
        .catch(err => dispatch(setError(err)) && err);
    } else {
      return Api.patch(`/customers/${customerId}/addresses/${state.addressId}`, formData)
        .then(address => dispatch(setError(null)) && address)
        .catch(err => dispatch(setError(err)) && err);
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [changeForm]: (state, [form, name, value]) => {
    const path = name === 'countryId' ? [form, name] : [form, 'formData', name];

    return assoc(state, path, value);
  },
  [assignAddress]: (form, state, address) => {
    const {region, ...formData} = address || {};

    formData.regionId = region && region.id;
    const countryId = region && region.countryId;

    return update(state, form, merge, {
      isAdding: false,
      formData,
      countryId,
      addressId: address.id
    });
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
    return assoc(state,
      [form, 'err'], err
    );
  }
}, initialState);

export default reducer;
