import ejs from 'elastic.js';
import _ from 'lodash';
import { post } from '../lib/search';

const adminSearchUrl = 'store_admins_search_view/_search';

export function searchAdmins(token) {
  const matchRule = ejs.Request().query(ejs.MatchAllQuery());
  const firstNameFilter = ejs.TermsFilter('firstName', [token]);
  const lastNameFilter = ejs.TermsFilter('lastName', [token]);
  const emailFilter = ejs.TermsFilter('email', [token]);

  matchRule.filter(ejs.OrFilter([firstNameFilter, lastNameFilter, emailFilter]));

  return post(adminSearchUrl, matchRule);
}
