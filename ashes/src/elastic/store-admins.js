import { post } from '../lib/search';
import * as dsl from './dsl';

const adminSearchUrl = 'store_admins_search_view/_search';

export function searchAdmins(token) {
  const caseInsesnitiveToken = token.toLowerCase();
  const matchRule = dsl.query({
    bool: {
      should: [
        dsl.termFilter('name', caseInsesnitiveToken),
        dsl.termFilter('email', caseInsesnitiveToken),
      ]
    }
  });

  return post(adminSearchUrl, matchRule);
}
