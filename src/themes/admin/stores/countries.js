'use strict';

import BaseStore from '../lib/base-store';

class CountryStore extends BaseStore {

  get baseUri() { return '/countries'; }

  regionName(countryCode) {
    return countryCode == 'US' ? 'State' : 'Region';
  }

  zipName(countryCode) {
    return countryCode == 'US' ? 'Zip Code' : 'Postal Code';
  }
}

export default new CountryStore();
