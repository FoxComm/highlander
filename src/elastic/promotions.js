/* @flow */

import _ from 'lodash';
import { post } from '../lib/search';
import * as dsl from './dsl';

const promotionsSearchUrl: string = 'promotions_search_view/_search';

export function searchPromotions(token: string): Promise {
  let matchRule = null;

  if (_.isNumber(token)) {
    const numberToken = parseInt(token);
    matchRule = dsl.query({
      bool: {
        must: [
          dsl.termFilter('applyType', 'coupon'),
        ],
        should: [
          dsl.termFilter('id', numberToken),
        ],
      },
    });
  } else {
    const caseInsesnitiveToken = token.toLowerCase();
    matchRule = dsl.query({
      bool: {
        must: [
          dsl.termFilter('applyType', 'coupon'),
        ],
        should: [
          dsl.termFilter('name', caseInsesnitiveToken),
        ],
      },
    });
  }

  return post(promotionsSearchUrl, matchRule);
}
