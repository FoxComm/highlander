import { createReducer } from 'redux-act';
import createAsyncActions from './async-utils';

import api from '../lib/api';

type State = {
  applied: boolean;
}

const { perform, ...actions } = createAsyncActions('applyFormSubmit', data => {
  return api.post('/merchant_applications', { merchant_application: { ...data } });
});

const initialState: State = {
  applied: false,
};

const reducer = createReducer({
  [actions.succeeded]: (state: State) => {
    return {
      ...state,
      applied: true,
    };
  },
}, initialState);

export {
  reducer as default,
  perform as submit,
};

