/* @flow */

import type { FormField } from '../../core/types/fields';

export const fields: Array<FormField> = [
  {
    name: 'business_name',
    type: 'input',
    placeholder: 'Business Name',
    validation: 'required',
  },
  {
    name: 'email_address',
    type: 'email',
    placeholder: 'Email Address',
    validation: 'required format.email',
  },
  {
    name: 'phone_number',
    type: 'text',
    placeholder: 'Phone Number',
    validation: 'required format.phone',
    mask: '+1 (999) 999-9999',
    maskChar: '_',
    normalize: value => value.replace(/[()]/g, '').replace(/\s/g, '-'),
  },
  {
    name: 'site_url',
    type: 'url',
    placeholder: 'Site URL',
    validation: 'required format.uri',
  },
];
