import Api from '../lib/api';
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';

export const toggleSiteMenu = createAction('SITE_MENU_TOGGLE');
export const toggleMenuItem = createAction('SITE_MENU_ITEM_TOGGLE');

export function getMenuItemState(item) {
  return (dispatch, getState) => {
    const state = getState();
    console.log(state);
    return _.get(state, ['siteMenu', 'menuItems', item], false);
  };
};

const initialState = {
  isMenuExpanded: true,
  menuItems: {}
};

const reducer = createReducer({
  [toggleSiteMenu]: (state) => {
    const currentState = _.get(state, 'isMenuExpanded', true);
    return {
      ...state,
      isMenuExpanded: !currentState
    };
  },
  [toggleMenuItem]: (state, item) => {
    console.log(item);
    const currentItemState = _.get(state, ['menuItems', item], false);
    return assoc(state, ['menuItems', item], !currentItemState);
  },
}, initialState);

export default reducer;
