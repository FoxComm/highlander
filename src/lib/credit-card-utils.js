import _ from 'lodash';

export function monthList() {
  return {
    '1': '01 - January',
    '2': '02 - February',
    '3': '03 - March',
    '4': '04 - April',
    '5': '05 - May',
    '6': '06 - June',
    '7': '07 - July',
    '8': '08 - August',
    '9': '09 - September',
    '10': '10 - October',
    '11': '11 - November',
    '12': '12 - December'
  };
}

export function expirationYears() {
  const current = new Date().getFullYear();

  return _.range(20).reduce( (years, n) => _.set(years, current + n,  (current + n).toString()), {});
}

export function formatExpiration(card) {
  return `${card.expMonth}/${card.expYear}`;
}


export function formatNumber(card) {
  return `xxxx xxxx xxxx ${card.lastFour}`;
}
