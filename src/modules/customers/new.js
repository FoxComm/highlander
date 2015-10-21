'use strict';

// state for customer adding form

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const changeFormData = createAction('CUSTOMER_NEW_CHANGE_FORM', (name, value) => ({name, value}));

const initialState = {
  email: null,
  name: null
};

const reducer = createReducer({
  [changeFormData]: (state, {name, value}) => {
    const newState = {
      ...state,
      [name]: value
    };

    return newState;
  }
}, initialState);

export default reducer;
