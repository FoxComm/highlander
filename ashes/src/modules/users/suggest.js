// libs
import _ from 'lodash';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { searchAdmins } from 'elastic/store-admins';

const _suggestAdmins = createAsyncActions(
  'suggestAdmins',
  term => searchAdmins(term)
);
export const suggestAdmins = _suggestAdmins.perform;

const initialState = {
  admins: [],
};

const reducer = createReducer({
  [_suggestAdmins.succeeded]: (state, response) => {
    return {
      ...state,
      admins: _.isEmpty(response.result) ? [] : response.result,
    };
  },
}, initialState);

export default reducer;

