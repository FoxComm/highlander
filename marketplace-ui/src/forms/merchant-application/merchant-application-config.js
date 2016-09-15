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
  },
  {
    name: 'phone_number',
    type: 'input',
    placeholder: 'Phone Number',
  },
  {
    name: 'email_address',
    type: 'input',
    placeholder: 'Email Address',
    validation: 'required email',
  },
  {
    name: 'monthly_sales',
    type: 'input',
    placeholder: 'Monthly Sales Volume',
  },
  {
    name: 'twitter',
    type: 'text',
    placeholder: 'Twitter Handle',
  },
  {
    name: 'site_url',
    type: 'text',
    placeholder: 'Site URL',
  },
  {
    name: 'audience',
    type: 'select',
    placeholder: 'Audience',
    values: LIST_AUDIENCE,
  },
  {
    name: 'category',
    type: 'select',
    placeholder: 'Category',
    values: LIST_CATEGORIES,
  },
];
