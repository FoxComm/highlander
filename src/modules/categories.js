/* @flow */

import { createAction, createReducer } from 'redux-act';

const categoriesFetchStated = createAction('CATEGORIES_FETCH_STARTED');
const categoriesFetchSucceded = createAction('CATEGORIES_FETCH_SUCCEDED');
const categoriesFetchFailed = createAction('CATEGORIES_FETCH_FAILED');

export function fetchCategories(): Function {
  return dispatch => {
    dispatch(categoriesFetchStated());

    const result = [
      {id: 2, name: 'eyeglasses'},
      {id: 3, name: 'sunglasses'},
      {id: 4, name: 'readers'},
    ];
    return dispatch(categoriesFetchSucceded(result));
  };
}

type Category = {
  id: number;
  name: string;
}

type FormData = {
  isFetching: boolean;
  list: Array<Category>;
}

const initialState: FormData = {
  isFetching: false,
  list: [],
};

const reducer = createReducer({
  [categoriesFetchStated]: state => {
    return {
      ...state,
      isFetching: true,
    };
  },
  [categoriesFetchSucceded]: (state, payload) => {
    return {
      ...state,
      list: payload,
      isFetching: false,
    };
  },
  [categoriesFetchFailed]: (state, err) => {
    console.error(err);

    return {
      ...state,
      isFetching: false,
    };
  },
}, initialState);

export default reducer;
