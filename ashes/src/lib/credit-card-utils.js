import _ from 'lodash';

export function monthList() {
  return [
    ['1', '01 - January'],
    ['2', '02 - February'],
    ['3', '03 - March'],
    ['4', '04 - April'],
    ['5', '05 - May'],
    ['6', '06 - June'],
    ['7', '07 - July'],
    ['8', '08 - August'],
    ['9', '09 - September'],
    ['10', '10 - October'],
    ['11', '11 - November'],
    ['12', '12 - December'],
  ];
}

export function expirationYears() {
  const current = new Date().getFullYear();

  return _.range(20).map(n => [current + n, current + n]);
}

export function formatExpiration({ expMonth, expYear }) {
  if (!expMonth || !expYear) {
    return 'N/A';
  }

  return `${expMonth}/${expYear}`;
}

export function formatNumber(card) {
  const lastFour = card.lastFour || 'xxxx';

  return `xxxx xxxx xxxx ${lastFour}`;
}

export function getBillingAddress(getState, customerId, addressId) {
  const state = getState();

  const addresses = _.get(state, `customers.addresses[${customerId}].addresses`, []);
  const billingAddress = _.find(addresses, address => address.id === addressId);
  const region = _.get(billingAddress, 'region', _.get(billingAddress, 'state', {}));

  billingAddress.regionId = region.id;
  billingAddress.state = region.name;
  billingAddress.country = _.get(_.find(state.countries, country => country.id === region.countryId), 'name', '');

  return billingAddress;
}
