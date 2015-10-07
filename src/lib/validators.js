'use strict';

import zipcodes from 'zipcodes-regex';

export function zipCode(value, country) {
  if (!zipcodes[country]) {
    return /^\w{1,9}$/.test(value);
  }

  return new RegExp(zipcodes[country]).test(value);
}

export function ascii(value) {
  return /[0-9a-zA-Z_\-]/.test(value) ? null : '$label must contain only ASCII characters';
}
