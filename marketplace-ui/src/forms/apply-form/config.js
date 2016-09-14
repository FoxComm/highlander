const LIST_AUDIENCE = [
  'Men', 'Women', 'Both', 'Kids',
];

const LIST_CATEGORIES = [
  'Accessories', 'Action', 'Sports', 'Activewear', 'Apparel', 'Beauty', 'Bridal', 'Eyewear',
  'Grooming', 'Handbags', 'Home', 'Intimates', 'Jeans', 'Jewelry', 'Kids', 'Shoes',
  'Sleepwear', 'Swimwear', 'Tech', 'Vintage',
];

export const fields = [
  {
    name: 'name',
    type: 'input',
    placeholder: 'Name',
  },
  {
    name: 'business_name',
    type: 'input',
    placeholder: 'Business Name',
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

export const validate = values => {
  const errors = {};
  if (!values.name) {
    errors.name = 'Required';
  }

  if (!values.business_name) {
    errors.business_name = 'Required';
  }

  if (!values.email_address) {
    errors.email_address = 'Required';
  } else if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i.test(values.email_address)) {
    errors.email_address = 'Invalid email address';
  }

  const urlRegex = /(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/i;

  if (!!values.twitter && !urlRegex.test(values.twitter)) {
    errors.twitter = 'Invalid URL';
  }

  if (!!values.site_url && !urlRegex.test(values.site_url)) {
    errors.site_url = 'Invalid URL';
  }

  return errors;
};
