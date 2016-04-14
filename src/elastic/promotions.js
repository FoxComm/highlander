/* @flow */

import { post } from '../lib/search';
import * as dsl from './dsl';

const promotionsSearchUrl: string = 'promotions_search_view/_search';

export function searchCouponPromotions(token: string): Promise {
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
      must: [
        dsl.termFilter('applyType', 'coupon'),
      ],
      should: filters,
      minimum_number_should_match: 1
    },
  });

  return post(promotionsSearchUrl, matchRule);
}
