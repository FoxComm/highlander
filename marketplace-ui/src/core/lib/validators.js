import isEmpty from 'lodash/isEmpty';

const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i;
const urlRegex = /(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/i;
const phoneRegex = /^\d{3}-\d{3}-\d{4}$/;

const required = value => !isEmpty(value);

const email = value => (value ? emailRegex.test(value) : true);

const uri = value => (value ? urlRegex.test(value) : true);

const phone = value => (value ? phoneRegex.test(value) : true);

export default {
  required,
  email,
  uri,
  phone,
};
