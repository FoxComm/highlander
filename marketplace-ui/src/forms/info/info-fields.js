/* @flow */
import get from 'lodash/get';

import type { FormField } from '../../core/types/fields';

const LIST_STATES = {
  Alabama: 'AL',
  Alaska: 'AK',
  'American Samoa': 'AS',
  Arizona: 'AZ',
  Arkansas: 'AR',
  California: 'CA',
  Colorado: 'CO',
  Connecticut: 'CT',
  Delaware: 'DE',
  'District Of Columbia': 'DC',
  'Federated States Of Micronesia': 'FM',
  Florida: 'FL',
  Georgia: 'GA',
  Guam: 'GU',
  Hawaii: 'HI',
  Idaho: 'ID',
  Illinois: 'IL',
  Indiana: 'IN',
  Iowa: 'IA',
  Kansas: 'KS',
  Kentucky: 'KY',
  Louisiana: 'LA',
  Maine: 'ME',
  'Marshall Islands': 'MH',
  Maryland: 'MD',
  Massachusetts: 'MA',
  Michigan: 'MI',
  Minnesota: 'MN',
  Mississippi: 'MS',
  Missouri: 'MO',
  Montana: 'MT',
  Nebraska: 'NE',
  Nevada: 'NV',
  'New Hampshire': 'NH',
  'New Jersey': 'NJ',
  'New Mexico': 'NM',
  'New York': 'NY',
  'North Carolina': 'NC',
  'North Dakota': 'ND',
  'Northern Mariana Islands': 'MP',
  Ohio: 'OH',
  Oklahoma: 'OK',
  Oregon: 'OR',
  Palau: 'PW',
  Pennsylvania: 'PA',
  'Puerto Rico': 'PR',
  'Rhode Island': 'RI',
  'South Carolina': 'SC',
  'South Dakota': 'SD',
  Tennessee: 'TN',
  Texas: 'TX',
  Utah: 'UT',
  Vermont: 'VT',
  'Virgin Islands': 'VI',
  Virginia: 'VA',
  Washington: 'WA',
  'West Virginia': 'WV',
  Wisconsin: 'WI',
  Wyoming: 'WY',
};

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
    type: 'select',
    placeholder: 'Legal Entity Address: State',
    values: Object.keys(LIST_STATES),
    normalize: value => get(LIST_STATES, value, ''),
  },
  {
    name: 'legal_entity_postal',
    type: 'input',
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
