
import _ from 'lodash';

export function regionName(countryCode) {
  // Canada have Provinces
  if (countryCode === 'CA') {
    return 'Province';
  }
  // USA & India have states
  return _.contains(['US', 'IN'], countryCode) ? 'State' : 'Region';
}

export function zipName(countryCode) {
  return countryCode === 'US' ? 'Zip Code' : 'Postal Code';
}

export function zipExample(countryCode) {
  switch (countryCode) {
    case 'US':
      return '11111';
    case 'CA':
      return 'A1A 1A1';
    case 'RU':
      return '111111';
  }
}

export function phoneExample(countryCode) {
  return '(111) 111-1111';
}

export function phoneMask(countryCode) {
  return '(999) 999-9999';
}
