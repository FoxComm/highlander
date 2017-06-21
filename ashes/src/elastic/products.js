/* @flow */

import { post } from '../lib/search';
import * as dsl from './dsl';
import moment from 'moment';
import rangeToFilter from './common';

// 1000 should be big enough to request all promotions with applyType = coupon
// without size parameter ES responds with 10 items max
const MAX_RESULTS = 1000;
const productsSearchUrl = `products_search_view/_search?size=${MAX_RESULTS}`;

type QueryOpts = {
  omitArchived: ?boolean,
  omitInactive: ?boolean,
};

function getArchivedFilter () {
  return dsl.existsFilter('archivedAt', 'missing');
};

function getInactiveFilters (esDate: string) {
  return {
    mustFilters: [
      dsl.existsFilter('activeFrom', 'exists'),
      dsl.rangeFilter('activeFrom', { 'lte': esDate }),
    ],
    shouldFilters: [
      dsl.existsFilter('activeTo', 'missing'),
      dsl.rangeFilter('activeTo', { 'gte': esDate }),
    ],
  }
};

function getTokenFilters (token: string) {
  if (isNaN(Number(token))) {
    return dsl.termFilter('title', token.toLowerCase());
  } else {
    const query = dsl.query({
      bool: {
        should: [
          dsl.termFilter('id', token),
          dsl.termFilter('title', token.toLowerCase()),
        ],
      },
    });
    return query;
  }
};

export function searchProducts(token: string, {
                                                omitArchived = false,
                                                omitInactive = false,
                                              }: ?QueryOpts): Promise<*> {
  const formattedDate = moment(Date.now()).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
  const esDate = `${formattedDate}||/d`;
  let must = [];
  let should = omitInactive ? [] : null;

  if (omitArchived) must = [ ...must, getArchivedFilter()];

  if (omitInactive) {
    const { mustFilters, shouldFilters } = getInactiveFilters(esDate);
    must = [ ...must, ...mustFilters];
    should = [ ...should, ...shouldFilters];
  }

  if (token) must = [ ...must, getTokenFilters(token)];

  const matchRule = dsl.query({
    bool: {
      must,
      should,
    },
  });

  return post(productsSearchUrl, matchRule);
}
