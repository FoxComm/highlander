/* @flow */

import Agni from 'lib/agni';
import * as dsl from './dsl';

const MAX_RESULTS = 1000;
const taxonomiesSearchUrl = `taxonomies_search_view?size=${MAX_RESULTS}`;

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

  return Agni.search(taxonomiesSearchUrl, matchRule);
}
