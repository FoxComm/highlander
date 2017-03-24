/* @flow */

import { post } from '../lib/search';
import * as dsl from './dsl';

// 1000 should be big enough to request all promotions with applyType = coupon
// without size parameter ES responds with 10 items max
const MAX_RESULTS = 1000;
const taxonomiesSearchUrl = `taxonomies_search_view/_search?size=${MAX_RESULTS}`;

export function searchTaxonomies(token: string): Promise<*> {
  const filters = [];
  if (token) {
    filters.push(
      dsl.termFilter('name', token.toLowerCase()),
    );
    if (!isNaN(Number(token))) {
      filters.push(
        dsl.termFilter('id', token)
      );
    }
  }

  const matchRule = dsl.query({
    bool: {
      must: filters,
      filter: dsl.existsFilter('archivedAt', 'missing')
    },
  });

  return post(taxonomiesSearchUrl, matchRule);
}

