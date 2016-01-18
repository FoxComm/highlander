import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

export const toggleSiteMenu = createAction('SITE_MENU_TOGGLE');

const initialState = {
  isMenuExpanded: true
};

const reducer = createReducer({
  [toggleSiteMenu]: (state) => {
    console.log(state);
    const currentState = _.get(state, 'isMenuExpanded', true);
    return {
      ...state,
      isMenuExpanded: !currentState
    };
  }
}, initialState);

export default reducer;
