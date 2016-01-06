import ejs from 'elastic.js';
import { DEFAULT_INDEX, newClient, rangeToFilter } from './common';
import _ from 'lodash';

const ADMINS_TYPE = 'store_admins_search_view';

const adminStartOpts = {
  index: DEFAULT_INDEX,
  type: ADMINS_TYPE,
};

export function searchAdmins(token) {
  const matchRule = ejs.Request().query(ejs.MatchAllQuery());
  const firstNameFilter = ejs.TermsFilter('firstName', [token]);
  const lastNameFilter = ejs.TermsFilter('lastName', [token]);
  const emailFilter = ejs.TermsFilter('email', [token]);

  matchRule.filter(ejs.OrFilter([firstNameFilter, lastNameFilter, emailFilter]));

  return newClient().search(_.merge(adminStartOpts, {
    body: matchRule,
    requestTimeout: 1000,
  }));
}
