/* @flow */

import type { FormField } from '../../core/types/fields';

export const LIST_SHIPPING_SOLUTIONS = ['FedEx', 'USPS', 'UPS'];

export const fields: Array<FormField> = [
  {
    name: 'shipping',
    type: 'select',
    multi: false,
    placeholder: 'Select from one of the shipping solutions below',
    values: LIST_SHIPPING_SOLUTIONS,
  },
];
