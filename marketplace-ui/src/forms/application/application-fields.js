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
    name: 'name',
    type: 'input',
    placeholder: 'Name',
    validation: 'required',
  },
  {
    name: 'business_name',
    type: 'input',
    placeholder: 'Business Name',
    validation: 'required',
  },
  {
    name: 'description',
    type: 'textarea',
    placeholder: 'Description',
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
    type: 'input',
    placeholder: 'Phone Number',
  },
  {
    name: 'monthly_sales_volume',
    type: 'input',
    placeholder: 'Monthly Sales Volume',
  },
  {
    name: 'twitter_handle',
    type: 'text',
    placeholder: 'Twitter Handle',
  },
  {
    name: 'instagram_handle',
    type: 'text',
    placeholder: 'Instagtam Handle',
  },
  {
    name: 'google_plus_handle',
    type: 'text',
    placeholder: 'Google Plus Handle',
  },
  {
    name: 'facebook_url',
    type: 'text',
    placeholder: 'Facebook URL',
    validation: 'uri',
  },
  {
    name: 'site_url',
    type: 'text',
    placeholder: 'Site URL',
    validation: 'uri',
  },
  {
    name: 'target_audience',
    type: 'select',
    placeholder: 'Audience',
    values: LIST_AUDIENCE,
  },
  {
    name: 'categories',
    type: 'select',
    placeholder: 'Category',
    values: LIST_CATEGORIES,
    normalize: (value) => [value],
  },
];
