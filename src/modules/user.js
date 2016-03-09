import { createAction, createReducer } from 'redux-act';

export const setUser = createAction('USER_SET');

const initialState = {};

const reducer = createReducer({
  [setUser]: (state, user) => {
    return {...state, ...user};
  },
}, initialState);

export default reducer;
