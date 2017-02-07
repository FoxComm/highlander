/* @flow */

import { toQuery } from './common';
import { post } from '../lib/search';

const MAX_RESULTS = 1000;
const mapping = 'customers_search_view/_search';
const searchUrl = `${mapping}?size=${MAX_RESULTS}`;

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

export function searchCustomers(exclides: Array<number>, token: string) {
  let filters = [];
  if (token) {
    const caseInsensitiveToken = token.toLowerCase();

    filters.push(dsl.termFilter('name', caseInsensitiveToken));
    filters.push(dsl.termFilter('email', caseInsensitiveToken));
    if (!isNaN(Number(token))) {
      filters.push(
        dsl.termFilter('id', token)
      );
    }
  }

  const matchRule = dsl.query({
    bool: {
      must: [
        dsl.termFilter('isGuest', 'false'),
      ],
      must_not: dsl.ids(excludeGroups),
      should: filters,
      minimum_number_should_match: 1,
    },
  });

  return post(searchUrl, matchRule);
}
