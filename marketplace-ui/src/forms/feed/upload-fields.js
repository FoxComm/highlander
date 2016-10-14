/* @flow */

import type { FormField } from '../../core/types/fields';

export const fields: Array<FormField> = [
  {
    name: 'file',
    type: 'file',
    placeholder: 'Select .XML, .CSV or .TXT file',
    validation: 'required',
  },
];
