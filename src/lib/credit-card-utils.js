import _ from 'lodash';

function monthList() {
  return {
    1: '01 - January',
    2: '02 - February',
    3: '03 - March',
    4: '04 - April',
    5: '05 - May',
    6: '06 - June',
    7: '07 - July',
    8: '08 - August',
    9: '09 - September',
    10: '10 - October',
    11: '11 - November',
    12: '12 - December'
  };
}

function expirationYears() {
  let years = {};
  const current = new Date().getFullYear();
  _.each(_.range(20), (inc) => {
    let year = (current + inc);
    years[year] = year;
  });
  return years;
}

export {monthList, expirationYears};
