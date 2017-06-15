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

function filterArchived (must: Array) {
  must.push(dsl.existsFilter('archivedAt', 'missing'));
};

function filterInactive (must: Array, should: Array, esDate: string) {
  must.push(dsl.existsFilter('activeFrom', 'exists'));
  must.push(dsl.rangeFilter('activeFrom', { 'lte': esDate }));
  should.push(dsl.existsFilter('activeTo', 'missing'));
  should.push(dsl.rangeFilter('activeTo', { 'gte': esDate }));
};

function addTokenQueries (token: string, must: Array) {
  if (isNaN(Number(token))) {
    must.push(dsl.termFilter('title', token.toLowerCase()));
  } else {
    const query = dsl.query({
      bool: {
        should: [
          dsl.termFilter('id', token),
          dsl.termFilter('title', token.toLowerCase()),
        ],
      },
    });
    must.push(query);
  }
};

export function searchProducts(token: string, {
                                                omitArchived = false,
                                                omitInactive = false,
                                              }: ?QueryOpts): Promise<*> {
  const formattedDate = moment(Date.now()).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
  const esDate = `${formattedDate}||/d`;
  const must = [];
  const should = omitInactive ? [] : null;

  if (omitArchived) filterArchived(must);

  if (omitInactive) filterInactive(must, should, esDate);

  if (token) addTokenQueries(token, must);

  const matchRule = dsl.query({
    bool: {
      must,
      should,
    },
  });

  return post(productsSearchUrl, matchRule);
}
