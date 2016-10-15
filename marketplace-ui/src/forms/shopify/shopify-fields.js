/* @flow */

import isEmpty from 'lodash/isEmpty';

import type { FormField } from '../../core/types/fields';

export const fields: Array<FormField> = [
  {
    name: 'shopifyKey',
    type: 'text',
    placeholder: 'Shopify Key',
    validation: 'required',
  },
  {
    name: 'shopifyPassword',
    type: 'text',
    placeholder: 'Shopify Password',
    validation: 'required',
  },
  {
    name: 'shopifyDomain',
    type: 'url',
    placeholder: 'Shopify Domain',
    validation: 'required format.uri',
  },
];