
import { dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import _ from 'lodash';
import { createAsyncActions } from '@foxcomm/wings';
import Api from 'lib/api';

export const addCount = createAction('ADD_SOME_COUNT');

const initialState = {
  count: 0,
  zzz: 'none',
};

const reducer = createReducer({
  [addCount]: state => {
    return {
      count: state.count + 1,
    };
  },
}, initialState);

export default reducer;
