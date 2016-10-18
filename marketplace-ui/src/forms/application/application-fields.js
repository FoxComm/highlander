/* @flow */

import type { FormField } from '../../core/types/fields';

const LIST_AUDIENCE = [
  'Men', 'Women', 'Both', 'Kids',
];

const LIST_CATEGORIES = [
  'Accessories', 'Action', 'Sports', 'Activewear', 'Apparel', 'Beauty', 'Bridal', 'Eyewear',
  'Grooming', 'Handbags', 'Home', 'Intimates', 'Jeans', 'Jewelry', 'Kids', 'Shoes',
  'Sleepwear', 'Swimwear', 'Tech', 'Vintage',
];

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
    name: 'monthly_sales_volume',
    type: 'text',
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
    type: 'url',
    placeholder: 'Site URL',
    validation: 'required format.uri',
  },
  {
    name: 'target_audience',
    type: 'tags',
    placeholder: 'Audience',
    validation: 'required',
    values: LIST_AUDIENCE,
  },
  {
    name: 'categories',
    type: 'tags',
    placeholder: 'Category',
    validation: 'required',
    values: LIST_CATEGORIES,
  },
];
