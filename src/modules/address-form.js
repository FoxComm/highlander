
import { assoc, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { fetchCountry } from './countries';

const DEFAULT_COUNTRY = 'US';

const changeForm = createAction('ADDRESS_FORM_CHANGE', (form, name, value) => [form, name, value]);
const assignAddress = createAction('ADDRESS_FORM_ASSIGN_ADDRESS', (form, address) => [form, address]);
const setNewCountry = createAction('ADDRESS_FORM_SET_NEW_COUNTRY', (form, country) => [form, country]);

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
      .then(dispatch(setNewCountry(form, getState().countries[countryId])));
  };
}

export function setAddress(form, address) {
  return (dispatch, getState) => {
    if (address) {
      dispatch(assignAddress(form, address));
    } else {
      dispatch(setCountry(form));
    }
  };
}

const initialState = {
  formData: {}
};

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
      formData,
      countryId
    });
  },
  [setNewCountry]: (state, [form, country]) => {
    return assoc(state,
      [form, 'countryId'], country.id,
      [form, 'formData', 'regionId'], country.regions[0].id
    );
  }
}, initialState);

export default reducer;
