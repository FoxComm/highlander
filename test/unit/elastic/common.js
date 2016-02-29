import _ from 'lodash';
import { assoc } from 'sprout-data';
import nock from 'nock';

const { toQuery } = importSource('elastic/common.js');

const baseSearch = {
  query: { 
    bool: {
      filter: void 0,
      must: void 0,
    },
  },
};

function addQuery(searchTerm, search = baseSearch) {
  const query = {
    match: {
      [searchTerm.term]: {
        max_expansions: 3,
        query: searchTerm.value.value,
        type: 'phrase_prefix',
      },
    },
  };

  const currentQueries = search.query.bool.must || [];
  return assoc(search, ['query', 'bool', 'must'], [...currentQueries, query]);
}

function addFilter(searchTerm, search = baseSearch) {
  const filter = {
    term: {
      [searchTerm.term]: searchTerm.value.value,
    },
  };

  const currentFilters = search.query.bool.filter || [];
  return assoc(search, ['query', 'bool', 'filter'], [...currentFilters, filter]);
}

describe('elastic.common', () => {
  describe('#toQuery', () => {
    it('should create a query with no parameters', () => {
      const query = toQuery();
      expect(query).to.eql({});
    });

    it('should create a query with a single string term', () => {
      const terms = [{
        term: 'name',
        operator: 'eq',
        value: { type: 'string', value: 'Fox' },
      }];

      const query = toQuery(terms);
      const expectedQuery = addQuery(terms[0]);
      expect(query).to.eql(expectedQuery);
    });

    it('should create a query with a single non-string term', () => {
      const terms = [{
        term: 'amount',
        operator: 'eq',
        value: { type: 'number', value: 1000 },
      }];

      const query = toQuery(terms);
      const expectedQuery = addFilter(terms[0]);
      expect(query).to.eql(expectedQuery);
    });

    it('should create a query with a string and non-string term', () => {
      const stringTerm = {
        term: 'name',
        operator: 'eq',
        value: { type: 'string', value: 'Fox' },
      };

      const nonStringTerm = {
        term: 'amount',
        operator: 'eq',
        value: { type: 'number', value: 1000 },
      };

      const terms = [stringTerm, nonStringTerm];
      const query = toQuery(terms);

      const expectedQuery = addQuery(stringTerm);
      const finalQuery = addFilter(nonStringTerm, expectedQuery);

      expect(query).to.eql(finalQuery);
    });

    it('should create a search with multiple terms and a sort order', () => {
      const stringTerm = {
        term: 'name',
        operator: 'eq',
        value: { type: 'string', value: 'Fox' },
      };

      const nonStringTerm = {
        term: 'amount',
        operator: 'eq',
        value: { type: 'number', value: 1000 },
      };

      const terms = [stringTerm, nonStringTerm];
      const query = toQuery(terms, { sortBy: 'name' });

      const expectedQuery = addQuery(stringTerm);
      const expectedFilter = addFilter(nonStringTerm, expectedQuery);

      const finalQuery = assoc(expectedFilter, 'sort', [{ name: { order: 'asc' } }]);
      expect(query).to.eql(finalQuery);
    });

    it('should create a search with a nested filter', () => {
      const terms = [{
        term: 'orders.referenceNumber',
        operator: 'eq',
        value: { type: 'string-term', value: 'BR10007' },
      }];

      const query = toQuery(terms);

      const expectedFilter = {
        query: {
          bool: {
            must: void 0,
            filter: [{
              nested: {
                path: 'orders',
                query: {
                  bool: {
                    filter: {
                      term: { 'orders.referenceNumber': 'br10007' },
                    },
                  },
                },
              },
            }],
          },
        },
      };

      expect(query).to.eql(expectedFilter);
    });

    it('should create a search with a nested query', () => {
      const terms = [{
        term: 'customer.name',
        operator: 'eq',
        value: { type: 'string', value: 'adil wali' },
      }];

      const query = toQuery(terms);

      const expectedQuery = {
        query: {
          bool: {
            must: void 0,
            filter: [{
              nested: {
                path: 'customer',
                query: {
                  bool: {
                    filter: {
                      match: { 'customer.name': 'adil wali' },
                    },
                  },
                },
              },
            }],
          },
        },
      };

      expect(query).to.eql(expectedQuery);
    });
  });
});
