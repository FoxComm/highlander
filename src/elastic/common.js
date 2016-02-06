import _ from 'lodash';
import ejs from 'elastic.js';
import moment from 'moment';

const MAX_EXPANSIONS = 10; // prevent long query


// TODO: filters are deprecated in favor to query and query filters
// TODO: also, some queries are changed or deprecated too in ES 2.x

function isNestedFilter(filter) {
  const term = filter.selectedTerm;
  if (!term) return false;
  return term.lastIndexOf('.') != -1;
}

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
 * @param {String} options.phrase - Adds Phrase prefix
 * @param {String} [options.joinWith=and] - and|or
 * @param {Boolean} [options.useQueryFilters=false] - Use FilteredQuery instead of filters
 * @returns The ElasticSearch query.
 */
export function toQuery(filters, options = {}) {
  const { phrase, useQueryFilters, joinWith } = options;

  const esFilters = _.chain(filters)
    .map(filter => {
      return isNestedFilter(filter)
        ? createNestedFilter(filter)
        : createFilter(filter, ejs.TermsFilter, rangeToFilter);
    })
    .filter()
    .value();

  const query = _.isEmpty(phrase) ? ejs.MatchAllQuery() : phrasePrefixQuery(phrase);
  const topJoinFilter = joinWith == 'or' ? ejs.OrFilter : ejs.AndFilter;

  if (_.isEmpty(esFilters)) {
    return ejs.Request().query(query);
  }

  if (useQueryFilters) {
    return ejs.Request().query(ejs.FilteredQuery(query, topJoinFilter(esFilters)));
  }

  return ejs.Request().query(query).filter(topJoinFilter(esFilters));
}

function phrasePrefixQuery(phrase, field = '_all') {
    return ejs.MatchQuery(field, phrase)
      .type('phrase_prefix')
      .maxExpansions(MAX_EXPANSIONS);
}

function createFilter(filter, boolFn, rangeFn) {
  const { selectedTerm, selectedOperator, value: { type, value } } = filter;

  switch(type) {
    case 'bool':
      return boolFn(selectedTerm, value);
    case 'bool_inverted':
      return boolFn(selectedTerm, !value);
    case 'currency':
    case 'enum':
    case 'number':
      return rangeFn(selectedTerm, selectedOperator, value);
    case 'string':
      return rangeFn(selectedTerm, selectedOperator, value.toLowerCase());
    case 'date':
      return dateRangeFilter(selectedTerm, selectedOperator, value, rangeFn);
  }
}

function createNestedFilter(filter) {
  const term = filter.selectedTerm;
  const path = term.slice(0, term.lastIndexOf('.'));
  const query = createFilter(
    filter,
    ejs.MatchQuery,
    rangeToQuery
  );

  return ejs
    .NestedFilter(path)
    .query(ejs.BoolQuery().must(query));
}

function dateRangeFilter(field, operator, value, rangeFn) {
  const formattedDate = moment(value, 'MM/DD/YYYY').format('YYYY-MM-DD HH:mm:ss');
  const esDate = `${formattedDate}||/d`;

  switch(operator) {
    case 'eq':
      const dates = [esDate, `${esDate}+1d`];
      return rangeFn(field, 'gte__lte', dates);
    case 'neq':
      return ejs.OrFilter([
        rangeFn(field, 'lt', esDate),
        rangeFn(field, 'gte', `${esDate}+1d`)
      ]);
    case 'lt':
    case 'gte':
      return rangeFn(field, operator, esDate);
    case 'lte':
    case 'gt':
      return rangeFn(field, operator, `${esDate}+1d`);
  }
}

function _rangeTo(field, operator, value, eqFn, rangeFn) {
  const filter = rangeFn(field);
  switch(operator) {
    case 'eq':
      return eqFn(field, value);
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
        const [op1, op2] = operator.split('__');
        filter[op1](value[0]);
        filter[op2](value[1]);
        break;
      }
      console.error('operator', operator, 'isn\'t suitable for value', value);
      break;

  }
  return filter;
}

export function rangeToFilter(field, operator, value) {
  return _rangeTo(field, operator, value, ejs.TermsFilter, ejs.RangeFilter);
}

export function rangeToQuery(field, operator, value) {
  return _rangeTo(field, operator, value, ejs.MatchQuery, ejs.RangeQuery);
}
