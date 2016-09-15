import createAsyncActions, { getActionState } from './async-utils';

import api from '../lib/api';

const ACTION_MERCHANT_APPLICATION_SUBMIT = 'merchantApplicationSubmit';

const { perform } = createAsyncActions(ACTION_MERCHANT_APPLICATION_SUBMIT, data =>
  api.post('/merchant_applications', { merchant_application: { ...data } })
);

const getApplyFormActionState = state => getActionState(state, ACTION_MERCHANT_APPLICATION_SUBMIT);

export {
  perform as submit,

  /* selectors */
  getApplyFormActionState,
};

