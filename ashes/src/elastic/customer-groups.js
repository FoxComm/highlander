/* @flow */

import { post } from '../lib/search';
import * as dsl from './dsl';

const MAX_RESULTS = 1000;
const searchUrl = `customer_groups_search_view/_search?size=${MAX_RESULTS}`;

export function searchGroups(excludeGroups: Array<number>, token: string) {
  let filters = [];
  if (token) {
    const caseInsensitiveToken = token.toLowerCase();

    filters.push(dsl.termFilter('name', caseInsensitiveToken));
    if (!isNaN(Number(token))) {
      filters.push(
        dsl.termFilter('id', token)
      );
    }
  }

  const matchRule = dsl.query({
    bool: {
      must: [
        dsl.existsFilter('deletedAt', 'missing'),
        dsl.termFilter('groupType', 'manual'),
      ],
      must_not: dsl.ids(excludeGroups),
      should: filters,
      minimum_number_should_match: 1,
    },
  });

  return post(searchUrl, matchRule);
}
