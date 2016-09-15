import createAsyncActions, { getActionState } from './async-utils';

import api from '../lib/api';

const { perform } = createAsyncActions('applyFormSubmit', data =>
  api.post('/merchant_applications', { merchant_application: { ...data } })
);

const getApplyFormActionState = state => getActionState(state, 'applyFormSubmit');

export {
  perform as submit,

  /* selectors */
  getApplyFormActionState,
};

