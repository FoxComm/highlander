'use strict';

import zipcodes from 'zipcodes-regex';

export function zipCode(value, country) {
  if (!zipcodes[country]) {
    return /^\w{1,9}$/.test(value);
  }

  return new RegExp(zipcodes[country]).test(value);
}
