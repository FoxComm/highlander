import { createAction, createReducer } from 'redux-act';

export const setUser = createAction('USER_SET');

const initialState = {};

export type TUser = {name: String, email: String};

const reducer = createReducer({
  [setUser]: (state, user: TUser) => {
    return {...state, ...user};
  },
}, initialState);

export default reducer;
