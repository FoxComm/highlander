/* @flow */

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

type FormData = {
  isVisible: boolean;
};

export const closeBanner = createAction('CLOSE_BANNER');

const initialState : FormData = {
  isVisible: true,
};

const reducer = createReducer({
  [closeBanner]: state => {
    const currentState = _.get(state, 'isVisible', false);
    return {
      ...state,
      isVisible: false,
    };
  },
}, initialState);

export default reducer;
