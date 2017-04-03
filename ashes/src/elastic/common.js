import _ from 'lodash';
import { map, flow, compact } from 'lodash/fp';
import moment from 'moment';
import * as dsl from './dsl';

// https://www.elastic.co/guide/en/elasticsearch/reference/current/breaking_20_query_dsl_changes.html
// https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0

/**
 * Converts search terms into a query to ElasticSearch.
 * @param {Object[]} filters An array of the Ashes version of a search terms.
 *  A filter is in the following format:
 *  {
 *    term: 'someTerm',
 *    operator: 'eq',
 *    value: {
 *      type: 'bool',
 *      value: true
 *    }
 *  }
 * @param {Object} [options] - Additional options for build query
 * @param {String} [options.phrase] - Adds Phrase prefix
 * @param {Boolean} [options.atLeastOne=false] - if is set to true only one matched filter is enough to success query
 * @param {String} [options.sortBy] - sorting field, can be `-field` for desc order or `field` for asc order
 * @param {Boolean} [options.sortRaw] - if to sort by 'raw' subfield for proper custom analized field sorting
 * @returns {Object} The ElasticSearch query.
 */
export function toQuery(filters, options = {}) {
  const { phrase, atLeastOne = false, sortBy = '', sortRaw = false } = options;

  if (_.isEmpty(filters) && _.isEmpty(phrase) && _.isEmpty(sortBy)) {
    return {};
  }

  const es = {
    filters: convertFilters(_.filter(filters, searchTerm => searchTerm.value.type !== 'string')),
    queries: convertFilters(_.filter(filters, searchTerm => searchTerm.value.type === 'string')),
  };

  if (!_.isEmpty(phrase)) {
    es.queries.push(dsl.matchQuery('_all', {
      query: phrase,
      type: 'phrase',
    }));
  }


  const qwery = {
    bool: {
      should: atLeastOne ? [
        ...es.queries,
        ...(_.isEmpty(es.filters) ? void 0 : es.filters) || []
      ] : void 0,
      filter: atLeastOne || _.isEmpty(es.filters) ? void 0 : es.filters,
      must: atLeastOne || _.isEmpty(es.queries) ? void 0 : es.queries,
    }
  };

  const sortParam = sortBy ? { sort: convertSorting(sortBy, sortRaw) } : null;
  return dsl.query(qwery, { ...sortParam });
}

export function addNativeFilters(req, filters) {
  if (!req.query) {
    req.query = { bool: { filter: [] } };
  }
  req.query.bool.filter = [
    ...(req.query.bool.filter || []),
    ...filters
  ];

  return req;
}

// add additional filters to query
export function addFilters(req, filters) {
  return addNativeFilters(req, convertFilters(filters));
}

function createFilter(filter) {
  const { term, operator, value: { type, value } } = filter;

  switch (type) {
    case 'bool':
      return dsl.termFilter(term, value);
    case 'bool_inverted':
      return dsl.termFilter(term, !value);
    case 'currency':
    case 'enum':
    case 'number':
    case 'term':
      return rangeToFilter(term, operator, value);
    case 'string':
      return dsl.matchQuery(term, {
        query: value,
        analyzer: 'standard',
        operator: 'and'
      });
    case 'phrase':
      return dsl.matchQuery(term, {
        query: value,
        type: 'phrase',
      });
    case 'identifier':
      return rangeToFilter(term, operator, value.toUpperCase());
    case 'date':
      return dateRangeFilter(term, operator, value);
    case 'exists':
      return dsl.existsFilter(term, operator);
  }
}

function isNestedFilter(filter) {
  const { term } = filter;

  if (!term) return false;
  return term.lastIndexOf('.') != -1;
}

function createNestedFilter(filter) {
  const { term } = filter;
  const path = term.slice(0, term.lastIndexOf('.'));
  const query = createFilter(filter);

  return dsl.nestedQuery(path, {
    bool: { filter: query }
  });
}

// uses nested strategy for nested filters

export function convertFilters(filters) {
  return flow(
    map(filter => isNestedFilter(filter) ? createNestedFilter(filter) : createFilter(filter)),
    compact
  )(filters);
}

function dateRangeFilter(field, operator, value) {
  const formattedDate = moment(value, 'MM/DD/YYYY').format('YYYY-MM-DDTHH:mm:ss.SSSZ');
  const esDate = `${formattedDate}||/d`;

  switch (operator) {
    case 'eq':
      return dsl.rangeFilter(field, {
        'gte': esDate,
        'lte': `${esDate}+1d`,
      });
    case 'neq':
      return { bool: { must_not: dateRangeFilter(field, 'eq', value) } };
    case 'lt':
    case 'gte':
      return rangeToFilter(field, operator, esDate);
    case 'lte':
    case 'gt':
      return rangeToFilter(field, operator, `${esDate}+1d`);
  }
}

export function rangeToFilter(field, operator, value) {
  if (operator == 'eq') {
    return dsl.termFilter(field, value);
  } else if (_.includes(operator, '__') && _.isArray(value)) {
    const [op1, op2] = operator.split('__');

    return dsl.rangeFilter(field, {
      [op1]: value[0],
      [op2]: value[1]
    });
  }

  return dsl.rangeFilter(field, {
    [operator]: value
  });
}

export function convertSorting(sortBy, sortRaw) {
  const field = sortBy.replace('-', '');
  const [parent, child] = field.split('.');
  const raw = sortRaw ? '.raw' : '';

  let order = {
    order: sortBy.charAt(0) == '-' ? 'desc' : 'asc'
  };

  if (child) {
    order = {
      ...order,
      nested_path: parent
    };
  }

  return [dsl.sortByField(`${field}${raw}`, order)];
}
