/* @flow */

import type { FormField } from '../../core/types/fields';

export const fields: Array<FormField> = [
  {
    name: 'first_name',
    type: 'text',
    placeholder: 'First Name',
    validation: 'required',
  },
  {
    name: 'last_name',
    type: 'text',
    placeholder: 'Last Name',
    validation: 'required',
  },
  {
    name: 'email_address',
    type: 'text',
    placeholder: 'Email Address',
    validation: 'required email',
  },
  {
    name: 'password',
    type: 'password',
    placeholder: 'Password',
    validation: 'required',
  },
];
