/* @flow */

import type { FormField } from '../../core/types/fields';

export const fields: Array<FormField> = [
  {
    name: 'account_number',
    type: 'input',
    placeholder: 'External Bank Account Data: Account number',
    validation: 'required',
  },
  {
    name: 'routing_number',
    type: 'input',
    placeholder: 'External Bank Account Data: Routing number',
    validation: 'required',
  },
  {
    name: 'city',
    type: 'input',
    placeholder: 'Legal Entity Address: City',
  },
  {
    name: 'state',
    type: 'input',
    placeholder: 'Legal Entity Address: State',
  },
  {
    name: 'zip',
    type: 'number',
    placeholder: 'Legal Entity Address: Postal Code',
  },
  {
    name: 'business_name',
    type: 'input',
    placeholder: 'Legal Entity Business Name',
  },
  {
    name: 'tax_id',
    type: 'input',
    placeholder: 'Legal Entity Tax ID',
  },
  {
    name: 'dob_day',
    type: 'input',
    placeholder: 'DOB of Business Rep: Day',
  },
  {
    name: 'dob_month',
    type: 'input',
    placeholder: 'DOB of Business Rep: Month',
  },
  {
    name: 'dob_year',
    type: 'input',
    placeholder: 'DOB of Business Rep: Year',
  },
  {
    name: 'ssn',
    type: 'input',
    placeholder: 'Business Rep: ssn Last 4',
  },
  {
    name: 'entity_type',
    type: 'input',
    placeholder: 'Business Entity Type',
  },
];
