import { assoc } from 'sprout-data';

const { toQuery } = requireSource('elastic/common.js');

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
    query_string: {
      analyze_wildcard: true,
      analyzer: 'standard',
      default_operator: 'AND',
      query: `*${searchTerm.value.value}*`,
    },
  };

  const currentQueries = search.query.bool.must || [];
  return assoc(search, ['query', 'bool', 'must'], [...currentQueries, query]);
}

function addFilter(searchTerm, search = baseSearch) {
  let value = searchTerm.value.value;
  if (searchTerm.value.type == 'string') {
    value = value.toLowerCase();
  }
  const filter = {
    term: {
      [searchTerm.term]: value,
    },
  };

  const currentFilters = search.query.bool.filter || [];
  return assoc(search, ['query', 'bool', 'filter'], [...currentFilters, filter]);
}

function omitUndefinedFields(obj) {
  return JSON.parse(JSON.stringify(obj));
}

describe('elastic.common', () => {
  describe('#toQuery', () => {
    it('should create a query with no parameters', () => {
      const query = toQuery();
      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields({}));
    });

    it('should create a query with a single string term', () => {
      const terms = [{
        term: 'name',
        operator: 'eq',
        value: { type: 'string', value: 'Fox' },
      }];

      const query = toQuery(terms);
      const expectedQuery = addQuery(terms[0]);
      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(expectedQuery));
    });

    it('should create a query with a single non-string term', () => {
      const terms = [{
        term: 'amount',
        operator: 'eq',
        value: { type: 'number', value: 1000 },
      }];

      const query = toQuery(terms);
      const expectedQuery = addFilter(terms[0]);
      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(expectedQuery));
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

      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(finalQuery));
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
      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(finalQuery));
    });

    it('should create a search with a nested filter', () => {
      const terms = [{
        term: 'orders.referenceNumber',
        operator: 'eq',
        value: { type: 'identifier', value: 'br10007' },
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
                      term: { 'orders.referenceNumber': 'BR10007' },
                    },
                  },
                },
              },
            }],
          },
        },
      };

      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(expectedFilter));
    });

    it('should create a search with a nested query', () => {
      const terms = [{
        term: 'customer.name',
        operator: 'eq',
        value: { type: 'identifier', value: 'Adil Wali' },
      }];

      const query = toQuery(terms);

      const expectedQuery = {
        query: {
          bool: {
            filter: [{
              nested: {
                path: 'customer',
                query: {
                  bool: {
                    filter: {
                      term: { 'customer.name': 'ADIL WALI' },
                    },
                  },
                },
              },
            }],
          },
        },
      };

      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(expectedQuery));
    });

    it('should create a search with nested sort', () => {
      const sortBy = '-customer.name';

      const query = toQuery([], { sortBy });

      const expectedQuery = {
        query: {
          bool: {}
        },
        sort: [{
          'customer.name': {
            order: 'desc',
            nested_path: 'customer'
          }
        }]
      };

      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(expectedQuery));
    });

    it('should create a search with raw subfield sort', () => {
      const sortBy = '-customer.name';

      const query = toQuery([], { sortBy, sortRaw: true });

      const expectedQuery = {
        query: {
          bool: {}
        },
        sort: [{
          'customer.name.raw': {
            order: 'desc',
            nested_path: 'customer'
          }
        }]
      };

      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(expectedQuery));
    });

    it('should create a search with exists filter', () => {
      const terms = [{
        term: 'name',
        operator: 'missing',
        value: { type: 'exists' },
      }];

      const query = toQuery(terms);

      const expectedQuery = {
        query: {
          bool: {
            filter: [{
              missing: { field: 'name' },
            }],
          },
        },
      };

      expect(omitUndefinedFields(query)).to.eql(omitUndefinedFields(expectedQuery));
    });
  });
});
