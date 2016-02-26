
import { createAction, createReducer } from 'redux-act';

const categoriesFetchStated = createAction('CATEGORIES_FETCH_STARTED');
const categoriesFetchSucceded = createAction('CATEGORIES_FETCH_SUCCEDED');
const categoriesFetchFailed = createAction('CATEGORIES_FETCH_FAILED');

export function fetchCategories() {
  return dispatch => {
    dispatch(categoriesFetchStated());

    const result = ['all', 'eyeglasses', 'sunglasses', 'readers'];
    return dispatch(categoriesFetchSucceded(result));
  };
}

const initialState = {};

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
