import { toQuery } from './common';
import { post } from '../lib/search';

const mapping = 'customers_search_view/_search';

export function groupCount(criteria, match) {
  return groupSearch(criteria, match, true);
}

export function groupSearch(criteria, match, forCount = false) {
  const req = toQuery(criteria, {atLeastOne: match == 'or'});
  if (forCount) {
    req.size = 0;
  }
  return post(mapping, req);
}
