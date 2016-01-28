import ejs from 'elastic.js';
import { toQuery } from './common';
import { post } from '../lib/search';
import _ from 'lodash';
import { assoc } from 'sprout-data';

const mapping = 'customers_search_view';


export function groupCount(criteria, match) {
  const req = toQuery(criteria, {joinWith: match, useQueryFilters: true});
  return post(mapping + '/_count', req.toJSON());
}

export function groupSearch(criteria, match) {
  const req = toQuery(criteria, {joinWith: match});
  return post(mapping + '/_search', req.toJSON());
}
