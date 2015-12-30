import _ from 'lodash';
import elasticsearch from 'elasticsearch';
import ejs from 'elastic.js';

/**
 * Converts search terms into a query to ElasticSearch.
 * @param {array} filters An array of the Ashes version of a search terms.
 *                A filter is in the following format:
 *  {
 *    selectedTerm: 'someTerm',
 *    selectedOperator: 'eq',
 *    value: {
 *      type: 'bool',
 *      value: true
 *    }
 *  }
 * @returns The ElasticSearch query.
 */
export function toQuery(filters) {
  const esFilters = _.map(filters, filter => {
    switch(filter.value.type) {
      case 'bool':
        return ejs.TermsFilter(
          filter.selectedTerm,
          filter.selectedOperator,
          filter.value.value
        );
      case 'currency':
      case 'date':
      case 'enum':
      case 'number':
      case 'string':
        return rangeToFilter(
          filter.selectedTerm,
          filter.selectedOperator,
          filter.value.value
        );
    }
  });

  return ejs
    .Request()
    .query(ejs.MatchAllQuery())
    .filter(ejs.AndFilter(esFilters));
}

function _newClient(opts = {}) {
  opts = _.merge({
    host: 'localhost:9200',
    apiVersion: '1.7',
  }, opts);
  return new elasticsearch.Client(opts);
}

export const newClient = _.memoize(_newClient);

export const DEFAULT_INDEX = 'phoenix';

export function rangeToFilter(field, operator, value) {
  const filter = ejs.RangeFilter(field);
  switch(operator) {
    case 'eq':
      return ejs.TermsFilter(field, value);
    case 'gt':
    case 'gte':
    case 'lte':
    case 'from':
    case 'to':
    case 'lt':
      filter[operator](value);
      break;
    default:
      if (_.contains(operator, '__') && _.isArray(value)) {
        const [op1,op2] = operator.split('__');
        filter[op1](value[0]);
        filter[op2](value[1]);
        break;
      }
      console.error('operator', operator, 'isn\'t suitable for value', value);
      break;

  }
  return filter;
}
