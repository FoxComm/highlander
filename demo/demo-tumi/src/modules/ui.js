// @flow

// application state created to serve not data but UI state itself

import { createAction, createReducer } from 'redux-act';
import { UPDATE_LOCATION } from 'react-router-redux';

type UIState = {
  sidebarVisible: boolean, // for mobile only
  searchVisible: boolean,
  usermenuVisible: boolean,
}

export const toggleSidebar = createAction('TOGGLE_SIDEBAR');
export const toggleSearch = createAction('TOGGLE_SEARCH');
export const toggleUserMenu = createAction('TOGGLE_USER_MENU');

const initialState: UIState = {
  sidebarVisible: false,
  searchVisible: false,
};

function toggleSearchState(state: UIState, shouldShow: ?boolean): UIState {
  const searchVisible = shouldShow == null ? !state.searchVisible : shouldShow;
  return {
    ...state,
    searchVisible,
    sidebarVisible: searchVisible ? false : state.sidebarVisible,
  };
}

const reducer = createReducer({
  [toggleSidebar]: (state: UIState) => {
    const sidebarVisible = !state.sidebarVisible;
    return {
      ...state,
      sidebarVisible,
      searchVisible: sidebarVisible ? false : state.searchVisible,
    };
  },
  [toggleSearch]: toggleSearchState,
  [toggleUserMenu]: (state: UIState) => {
    return {
      ...state,
      usermenuVisible: !state.usermenuVisible,
    };
  },
  [UPDATE_LOCATION]: (state: UIState) => {
    return toggleSearchState(state, false);
  },
}, initialState);

export default reducer;
