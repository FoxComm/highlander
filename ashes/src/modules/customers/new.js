// @flow

import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

export type NewCustomerPayload = {
  email: string,
  name: string,
}

const _createCustomer = createAsyncActions(
  'createCustomer',
  (customerPayload: NewCustomerPayload) => {
    return Api.post('/customers', customerPayload);
  }
);

export const createCustomer = _createCustomer.perform;
export const clearErrors = _createCustomer.clearErrors;

