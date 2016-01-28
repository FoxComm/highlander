import ejs from 'elastic.js';
import { DEFAULT_INDEX, newClient, toQuery } from './common';
import _ from 'lodash';
import { assoc } from 'sprout-data';

const CUSTOMERS_TYPE = 'customers_search_view';

const customersStartOpts = {
  index: DEFAULT_INDEX,
  type: CUSTOMERS_TYPE,
};


export function groupCount(criteria, match) {
  const req = toQuery(criteria, {joinWith: match, useQueryFilters: true});
  return newClient().count(_.merge(customersStartOpts, {
      body: req,
      requestTimeout: 1000,
    }
  ));
}

export function groupSearch(criteria, match) {
  const req = toQuery(criteria, {joinWith: match});
  return newClient().search(_.merge(customersStartOpts, {
      body: req
    }
  ));
}
