
import { assoc, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { fetchCountry } from  './countries';

const changeForm = createAction('ADDRESS_FORM_CHANGE', (name, value) => [name, value]);
export const assignAddress = createAction('ADDRESS_FORM_ASSIGN_ADDRESS');

export function changeValue(name, value) {
  return dispatch => {

  };
}


const initialState = {
  formData: {}
};

const reducer = createReducer({
  [changeForm]: (state, [name, value]) => {
    const path = name === 'countryId' ? [name] : ['formData', name];

    return assoc(state, path, value);
  },
  [assignAddress]: (state, address) => {
    const {region, ...formData} = address || {};

    formData.regionId = region && region.id;
    const countryId = region && region.countryId;

    return merge(state, {
      formData,
      countryId
    });
  }
}, initialState);

export default reducer;
