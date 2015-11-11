
import { assoc, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export const changeValue = createAction('ADDRESS_FORM_CHANGE', (name, value) => [name, value]);
export const assignAddress = createAction('ADDRESS_FORM_ASSIGN_ADDRESS');


const initialState = {
  formData: {}
};

const reducer = createReducer({
  [changeValue]: (state, [name, value]) => {
    return assoc(state, ['formData', name], value);
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
