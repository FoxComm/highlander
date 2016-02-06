import _ from 'lodash';
import { post } from '../lib/search';
import * as dsl from './dsl';

const adminSearchUrl = 'store_admins_search_view/_search';

export function searchAdmins(token) {
  const matchRule = dsl.query({
    bool: {
      should: [
        dsl.termFilter('firstName', token),
        dsl.termFilter('lastName', token),
        dsl.termFilter('email', token),
      ]
    }
  });

  return post(adminSearchUrl, matchRule);
}
