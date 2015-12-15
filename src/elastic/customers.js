import ejs from 'elastic.js';
import { DEFAULT_INDEX, newClient, rangeToFilter } from './common';
import _ from 'lodash';


const CUSTOMERS_TYPE = 'customers_search_view';

const customersStartOpts = {
  index: DEFAULT_INDEX,
  type: CUSTOMERS_TYPE,
};


export function groupCriteriaToRequest(criteria, match) {
  const filters = _.chain(criteria)
    .map((crit, _) => {
    switch (crit.value.type) {
      case 'bool':
        return ejs.TermsFilter(crit.selectedTerm, crit.value.value);
      case 'date':
      case 'number':
      case 'currency':
        return rangeToFilter(crit.selectedTerm, crit.selectedOperator, crit.value.value);

    }
  })
  .filter()
  .value();

  let matchFilter;
  switch(match) {
    case 'one':
      matchFilter = ejs.OrFilter;
      break;
    case 'all':
    default:
      matchFilter = ejs.AndFilter;
      break;
  }

  if (!_.isEmpty(filters)) {
    return ejs.Request().query(ejs.FilteredQuery(ejs.MatchAllQuery(), matchFilter(filters)));
  }
  return  ejs.Request().query(ejs.MatchAllQuery());
}

export function groupCount(criteria, match) {
  const req = groupCriteriaToRequest(criteria, match);
  return newClient().count(_.merge(customersStartOpts, {
      body: req,
      requestTimeout: 1000,
    }
  ));
}

export function groupSearch(criteria, match) {
  const req = groupCriteriaToRequest(criteria, match);
  return newClient().search(_.merge(customersStartOpts, {
      body: req
    }
  ));
}
