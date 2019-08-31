/* @flow */

import { toQuery } from './common';
import Agni from 'lib/agni';
import * as dsl from './dsl';

const MAX_RESULTS = 1000;
const mapping = 'customers_search_view';
const searchUrl = `${mapping}?size=${MAX_RESULTS}`;

export function groupCount(criteria: Object, match: string) {
  return groupSearch(criteria, match, true);
}

export function groupSearch(criteria: Object, match: string, forCount: boolean = false) {
  const req = toQuery(criteria, {atLeastOne: match == 'or'});
  if (forCount) {
    req.size = 0;
  }
  return Agni.search(mapping, req);
}

export function searchCustomers(excludes: Array<number>, token: string) {
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
      must_not: dsl.ids(excludes),
      should: filters,
      minimum_number_should_match: 1,
    },
  });

  return Agni.search(searchUrl, matchRule);
}
