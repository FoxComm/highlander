/* @flow */

import get from 'lodash/get';

import type { FormField } from '../../core/types/fields';

const LIST_AUDIENCE = [
  'Men', 'Women', 'Both', 'Kids',
];

const LIST_CATEGORIES = [
  'Accessories', 'Action', 'Sports', 'Activewear', 'Apparel', 'Beauty', 'Bridal', 'Eyewear',
  'Grooming', 'Handbags', 'Home', 'Intimates', 'Jeans', 'Jewelry', 'Kids', 'Shoes',
  'Sleepwear', 'Swimwear', 'Tech', 'Vintage',
];

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
    name: 'business_name',
    type: 'input',
    placeholder: 'Business Name',
    validation: 'required',
  },
  {
    name: 'email_address',
    type: 'input',
    placeholder: 'Email Address',
    validation: 'required email',
  },
  {
    name: 'phone_number',
    type: 'phone',
    placeholder: 'Phone Number',
    validation: 'required phone',
    normalize: value => value.replace(/^\+1\s?/, '').replace(/[()]/g, '').replace(/\s/g, '-'),
  },
  {
    name: 'monthly_sales_volume',
    type: 'input',
    placeholder: 'Monthly Sales Volume',
    validation: 'required',
  },
  {
    name: 'twitter_handle',
    type: 'text',
    placeholder: 'Twitter Handle',
  },
  {
    name: 'site_url',
    type: 'text',
    placeholder: 'Site URL',
    validation: 'required uri',
  },
  {
    name: 'target_audience',
    type: 'select',
    placeholder: 'Audience',
    validation: 'required',
    values: LIST_AUDIENCE,
  },
  {
    name: 'categories',
    type: 'select',
    placeholder: 'Category',
    validation: 'required',
    values: LIST_CATEGORIES,
  },
  {
    name: 'legal_entity_state',
    type: 'select',
    placeholder: 'Legal Entity Address: State',
    values: Object.keys(LIST_STATES),
    format: value => (value ? [value] : null),
    normalize: value => {
      const iso = Array.isArray(value) ? value.pop() : value;
      const state = get(LIST_STATES, iso, null);

      return state;
    },
  },
];
