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
    name: 'first_name',
    type: 'input',
    placeholder: 'First Name',
    validation: 'required',
  },
  {
    name: 'last_name',
    type: 'input',
    placeholder: 'Last Name',
    validation: 'required',
  },
  {
    name: 'phone_number',
    type: 'input',
    placeholder: 'Phone Number',
  },
  {
    name: 'business_name',
    type: 'input',
    placeholder: 'Business Name',
    validation: 'required',
  },
  {
    name: 'site_url',
    type: 'text',
    placeholder: 'Website',
  },
  {
    name: 'description',
    type: 'textarea',
    placeholder: 'Description',
  },
  {
    name: 'email_address',
    type: 'input',
    placeholder: 'Email Address',
    validation: 'required email',
  },
  {
    name: 'password',
    type: 'password',
    placeholder: 'Password',
  },
];
