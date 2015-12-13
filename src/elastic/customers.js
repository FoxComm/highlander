import ejs from 'elastic.js';
import { DEFAULT_INDEX, newClient, rangeToFilter } from './common';
import _ from 'lodash';


const CUSTOMERS_TYPE = 'customers_search_view';

const customersStartOpts = {
  index: DEFAULT_INDEX,
  type: CUSTOMERS_TYPE,
};


export function groupCriteriaToRequest(criteria) {
  const filters = _.map(criteria, (crit, _) => {
    switch (crit.value.type) {
      case 'bool':
        return ejs.TermsFilter(crit.selectedTerm, crit.value.value);
      case 'date':
      case 'number':
      case 'currency':
        return rangeToFilter(crit.selectedTerm, crit.selectedOperator, crit.value.value);

    }
  });
  return ejs.Request().query(ejs.FilteredQuery(ejs.MatchAllQuery(),ejs.AndFilter(filters)));
}

export function groupCount(criteria) {
  const req = groupCriteriaToRequest(criteria);
  return newClient().count(_.merge(customersStartOpts, {
      body: req
    }
  ));
}

export function groupSearch(criteria) {
  const req = groupCriteriaToRequest(criteria);
  return newClient().search(_.merge(customersStartOpts, {
      body: req
    }
  ));
}
