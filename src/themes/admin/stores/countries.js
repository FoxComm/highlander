'use strict';

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
      res = this._sort(res, 'name')
    }
    super._update(models, res);
  }

  regionName(countryCode) {
    return _.contains(['US', 'IN'], countryCode) ? 'State' : 'Region';
  }

  countryName(id) {
    let country = this.findWhere({id});
    return country && country.name;
  }

  zipName(countryCode) {
    return countryCode === 'US' ? 'Zip Code' : 'Postal Code';
  }
}

export default new CountryStore();
