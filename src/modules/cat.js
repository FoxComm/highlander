
import { createReducer, createAction } from 'redux-act';

const setName = createAction('SET_CAT_NAME');
const startLoading = createAction();
const stopLoading = createAction();

export function findName() {
  return dispatch => {
    dispatch(startLoading());
    return new Promise(resolve => {
      setTimeout(() => {
        dispatch(setName('Mushika'));
        dispatch(stopLoading());
        resolve();
      }, 2000);
    });
  };
}

const reducer = createReducer({
  [startLoading]: state => {
    return {
      ...state,
      loading: true,
    }
  },
  [stopLoading]: state => {
    return {
      ...state,
      loading: false,
    }
  },
  [setName]: (state, name) => {
    return {
      ...state,
      name
    };
  },
}, {});

export default reducer;
