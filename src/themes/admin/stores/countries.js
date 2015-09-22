'use strict';

import _ from 'lodash';
import BaseStore from '../lib/base-store';

class CountryStore extends BaseStore {

  get baseUri() { return '/countries'; }

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

  zipName(countryCode) {
    return countryCode === 'US' ? 'Zip Code' : 'Postal Code';
  }
}

export default new CountryStore();
