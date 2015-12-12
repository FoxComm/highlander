import ejs from 'elastic.js';
import { DEFAULT_INDEX, newClient, rangeToFilter } from './common';
import _ from 'lodash';


const CUSTOMERS_TYPE = 'customers_search_view';

const customersStartOpts = {
  index: DEFAULT_INDEX,
  type: CUSTOMERS_TYPE,
};

export function searchCustomers(criterions) {
  const req = ejs.Request().query(ejs.MatchAllQuery());
  const filters = _.map(criterions, (crit, _) => {
    switch(crit.value.type) {
      case 'bool':
        return ejs.TermsFilter(crit.selectedTerm, crit.value.value == 't'); // FIXME: use native bool?
      case 'date':
      case 'number':
      case 'currency':
        return rangeToFilter(crit.selectedTerm, crit.selectedOperator, crit.value.value);

    }
  });
  req.filter(ejs.AndFilter(filters));

  return newClient().search(_.merge(customersStartOpts, {
      body: req
    }
  ));
}
