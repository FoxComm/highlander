/* @flow */

import type { FormField } from '../../core/types/fields';

export const fields: Array<FormField> = [
  {
    name: 'bank_account_number',
    type: 'input',
    placeholder: 'External Bank Account Data: Account number',
    validation: 'required',
  },
  {
    name: 'bank_routing_number',
    type: 'input',
    placeholder: 'External Bank Account Data: Routing number',
    validation: 'required',
  },
  {
    name: 'legal_entity_name',
    type: 'input',
    placeholder: 'Legal Entity Business Name',
    validation: 'required',
  },
  {
    name: 'legal_entity_city',
    type: 'input',
    placeholder: 'Legal Entity Address: City',
  },
  {
    name: 'legal_entity_state',
    type: 'input',
    placeholder: 'Legal Entity Address: State',
  },
  {
    name: 'legal_entity_postal',
    type: 'number',
    placeholder: 'Legal Entity Address: Postal Code',
  },
  {
    name: 'legal_entity_tax_id',
    type: 'input',
    placeholder: 'Legal Entity Tax ID',
  },
  {
    name: 'business_founded_day',
    type: 'input',
    placeholder: 'DOB of Business Rep: Day',
  },
  {
    name: 'business_founded_month',
    type: 'input',
    placeholder: 'DOB of Business Rep: Month',
  },
  {
    name: 'business_founded_year',
    type: 'input',
    placeholder: 'DOB of Business Rep: Year',
  },
  {
    name: 'representative_ssn_trailing_four',
    type: 'input',
    placeholder: 'Business Rep: ssn Last 4',
  },
  {
    name: 'legal_entity_type',
    type: 'input',
    placeholder: 'Business Entity Type',
  },
];
