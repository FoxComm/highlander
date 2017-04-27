/* @flow */

import { post } from '../lib/search';
import * as dsl from './dsl';

// 1000 should be big enough to request all promotions with applyType = coupon
// without size parameter ES responds with 10 items max
const MAX_RESULTS = 1000;
const promotionsSearchUrl: string = `promotions_search_view/_search?size=${MAX_RESULTS}`;

export function searchCouponPromotions(token: string): Promise<*> {
  const filters = [];
  if (token) {
    const caseInsensitiveToken = token.toLowerCase();
    const terms = [
      dsl.termFilter('promotionName', caseInsensitiveToken),
      dsl.termFilter('storefrontName', caseInsensitiveToken),
    ];
    filters.push(...terms);
    if (!isNaN(Number(token))) {
      filters.push(
        dsl.termFilter('id', token)
      );
    }
  }
  const matchRule = dsl.query({
    bool: {
      must: [
        dsl.termFilter('applyType', 'coupon'),
      ],
      should: filters,
      minimum_number_should_match: 1,
    },
  });

  return post(promotionsSearchUrl, matchRule);
}
