/* @flow */

import { post } from '../lib/search';
import * as dsl from './dsl';
import moment from 'moment';
import rangeToFilter from './common';

// 1000 should be big enough to request all promotions with applyType = coupon
// without size parameter ES responds with 10 items max
const MAX_RESULTS = 1000;
const productsSearchUrl = `products_search_view/_search?size=${MAX_RESULTS}`;

export function searchProducts(token: string,
  omitArchived: boolean = false, omitInactive: boolean = false): Promise<*> {
  const formattedDate = moment(Date.now()).format('YYYY-MM-DDTHH:mm:ss.SSSZ');
  const esDate = `${formattedDate}||/d`;
  const must = [];
  const should = omitInactive ? [] : null;

  if (omitArchived) {
    must.push(
      dsl.existsFilter('archivedAt', 'missing')
    );
  }

  if (omitInactive) {
    must.push(
      dsl.existsFilter('activeFrom', 'exists')
    );
    must.push(
      dsl.rangeFilter('activeFrom', {
        'lte': esDate,
      })
    );
    should.push(
      dsl.existsFilter('activeTo', 'missing')
    );
    should.push(
      dsl.rangeFilter('activeTo', {
        'gte': esDate,
      })
    );
  }

  if (token) {
    if (isNaN(Number(token))) {
      must.push(
        dsl.termFilter('title', token.toLowerCase()),
      );
    } else {
      const shouldForToken = [];
      shouldForToken.push(
        dsl.termFilter('id', token)
      );
      shouldForToken.push(
        dsl.termFilter('title', token.toLowerCase())
      );
      const query =  dsl.query({
        bool: {
          should: shouldForToken,
        },
      });
      must.push(query);
    }
  }

  const matchRule = dsl.query({
    bool: {
      must,
      should,
    },
  });

  return post(productsSearchUrl, matchRule);
}
