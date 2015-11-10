import _ from 'lodash';
import BaseStore from '../lib/base-store';

class CountryStore extends BaseStore {

  get baseUri() { return '/countries'; }

  fetch(id) {
    return this._lastFetch = super.fetch(id);
  }

  lazyFetch() {
    if (!this._lastFetch) {
      return this.fetch();
    }
    return this._lastFetch;
  }

  // always sort countries after fetch
  _update(models, res) {
    if (_.isArray(res)) {
      res = this._sort(res, 'name');
    }
    super._update(models, res);
  }

  regionName(countryCode) {
    // Canada have Provinces
    if (countryCode === 'CA') {
      return 'Province';
    }
    // USA & India have states
    return _.contains(['US', 'IN'], countryCode) ? 'State' : 'Region';
  }

  countryName(id) {
    let country = this.findWhere({id});
    return country && country.name;
  }

  zipName(countryCode) {
    return countryCode === 'US' ? 'Zip Code' : 'Postal Code';
  }

  zipExample(countryCode) {
    switch (countryCode) {
      case 'US':
        return '11111';
      case 'CA':
        return 'A1A 1A1';
      case 'RU':
        return '111111';
    }
  }

  phoneExample(countryCode) {
    return '(111) 111-1111';
  }

  phoneMask(countryCode) {
    return '(999) 999-9999';
  }
}

export default new CountryStore();
