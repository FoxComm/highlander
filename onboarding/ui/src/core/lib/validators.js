import isEmpty from 'lodash/isEmpty';
import negate from 'lodash/negate';

const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i;
const uriRegex = /(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/i;
const phoneRegex = /^\+\d{1,2}-\d{3}-\d{3}-\d{4}$/;
const routingRegex = /^\d{9}$/;
const zipRegex = /^\d{5}(-\d{4})?$/;
const ssnRegex = /^\d{4}$/;

const validateFormat = regex => value => (value ? regex.test(value) : true);

const required = negate(isEmpty);
const email = validateFormat(emailRegex);
const uri = validateFormat(uriRegex);
const phone = validateFormat(phoneRegex);
const routing = validateFormat(routingRegex);
const zip = validateFormat(zipRegex);
const ssn = validateFormat(ssnRegex);

export default {
  required,
  format: {
    email,
    uri,
    phone,
    zip,
    routing_number: routing,
    SSN_last_four: ssn,
  },
};
