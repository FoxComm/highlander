import _ from 'lodash';
import moment from 'moment';
import * as dsl from './dsl';

// https://www.elastic.co/guide/en/elasticsearch/reference/current/breaking_20_query_dsl_changes.html
// https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0

/**
 * Converts search terms into a query to ElasticSearch.
 * @param {Object[]} filters An array of the Ashes version of a search terms.
 *  A filter is in the following format:
 *  {
 *    selectedTerm: 'someTerm',
 *    selectedOperator: 'eq',
 *    value: {
 *      type: 'bool',
 *      value: true
 *    }
 *  }
 * @param {Object} [options] - Additional options for build query
 * @param {String} [options.phrase] - Adds Phrase prefix
 * @param {Boolean} [options.atLeastOne=false] - if is set to true only one matched filter is enough to success query
 * @returns The ElasticSearch query.
 */
export function toQuery(filters, options = {}) {
  const { phrase, atLeastOne = false } = options;

  const boolQuery = {
    bool: {
      must: _.isEmpty(phrase) ? void 0 : dsl.matchQuery(phrase),
      [atLeastOne ? 'should' : 'filter']: convertFilters(filters),
    },
  };

  return dsl.query(boolQuery);
}

// add additional filters to query
export function addFiltersToQuery(query, filters) {
  query.bool.filter = [
    ...(query.bool.filter || []),
    ...convertFilters(filters)
  ];

  return query;
}

function createFilter(filter) {
  const { selectedTerm, selectedOperator, value: { type, value } } = filter;

  switch(type) {
    case 'bool':
      return dsl.termFilter(selectedTerm, value);
    case 'bool_inverted':
      return dsl.termFilter(selectedTerm, !value);
    case 'currency':
    case 'enum':
    case 'number':
      return rangeToFilter(selectedTerm, selectedOperator, value);
    case 'string':
      return rangeToFilter(selectedTerm, selectedOperator, value.toLowerCase());
    case 'date':
      return dateRangeFilter(selectedTerm, selectedOperator, value);
  }
}

function isNestedFilter(filter) {
  const term = filter.selectedTerm;
  if (!term) return false;
  return term.lastIndexOf('.') != -1;
}

function createNestedFilter(filter) {
  const term = filter.selectedTerm;
  const path = term.slice(0, term.lastIndexOf('.'));
  const query = createFilter(filter);

  return dsl.nestedQuery(path, {
    bool: {must: query}
  });
}

// uses nested strategy for nested filters

export function convertFilters(filters) {
  return _.chain(filters)
    .map(filter => isNestedFilter(filter) ? createNestedFilter(filter) : createFilter(filter))
    .compact()
    .value();
}

function dateRangeFilter(field, operator, value) {
  const formattedDate = moment(value, 'MM/DD/YYYY').format('YYYY-MM-DD HH:mm:ss');
  const esDate = `${formattedDate}||/d`;

  switch(operator) {
    case 'eq':
      const dates = [esDate, `${esDate}+1d`];
      return rangeToFilter(field, 'gte__lte', dates);
    case 'neq':
      return {bool: {must_not: dateRangeFilter(field, 'eq', value)}};
    case 'lt':
    case 'gte':
      return rangeToFilter(field, operator, esDate);
    case 'lte':
    case 'gt':
      return rangeToFilter(field, operator, `${esDate}+1d`);
  }
}

export function rangeToFilter(field, operator, value) {
  switch(operator) {
    case 'eq':
      return dsl.termFilter(field, value);
    case 'gt':
    case 'gte':
    case 'lte':
    case 'from':
    case 'to':
    case 'lt':
      return dsl.rangeFilter(field, {
        [operator]: value
      });
      break;
    default:
      if (_.contains(operator, '__') && _.isArray(value)) {
        const [op1, op2] = operator.split('__');

        return dsl.rangeFilter(field, {
          [op1]: value[0],
          [op2]: value[1],
        });
      }
      console.error('operator', operator, 'isn\'t suitable for value', value);
  }
}
