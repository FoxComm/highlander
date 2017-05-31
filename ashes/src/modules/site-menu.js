import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';

export const toggleSiteMenu = createAction('SITE_MENU_TOGGLE');
export const toggleMenuItem = createAction('SITE_MENU_ITEM_TOGGLE', (item, status) => [item, status]);
export const resetManuallyChanged = createAction('SITE_MENU_RESET_MANUALLY_CHAGED');

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
  [toggleMenuItem]: (state, [item, status]) => {
    let currentItemState;
    if (status != undefined) {
      currentItemState = status;
    } else {
      currentItemState = _.get(state, ['menuItems', item, 'isOpen'], false);
    }
    return assoc(state,
      ['menuItems', item, 'isOpen'], !currentItemState,
      ['menuItems', item, 'toggledManually'], true
    );
  },
  [resetManuallyChanged]: (state) => {
    let newItems = {};
    for (const key in state.menuItems) {
      const item = state.menuItems[key];
      const newItem = assoc(item, 'toggledManually', false);
      newItems = {
        ...newItems,
        key: newItem
      };
    }
    return {
      ...state,
      menuItems: newItems
    };
  },
}, initialState);

export default reducer;
