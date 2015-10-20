'use strict';

// state for customer adding form

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

const initialState = {
  email: null,
  name: null
};

const reducer = createReducer({}, initialState);

export default reducer;
