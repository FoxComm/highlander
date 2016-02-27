import _ from 'lodash';
import { assoc } from 'sprout-data';
import nock from 'nock';

const { toQuery } = importSource('elastic/common.js');

const baseQuery = {
  query: { bool: {} },
};

function composeQuery(query) {
  return assoc(baseQuery, ['query', 'bool'], query);
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
      const expectedQuery = {
        must: [{
          match: {
            name: 'Fox',
          },
        }],
      };

      expect(query).to.eql(composeQuery(expectedQuery));
    });

    it('should create a query with a single non-string term', () => {
      const terms = [{
        term: 'amount',
        operator: 'eq',
        value: { type: 'number', value: 1000 },
      }];

      const query = toQuery(terms);
      const expectedQuery = {
        filter: [{
          term: { amount: 1000 },
        }],
      };

      expect(query).to.eql(composeQuery(expectedQuery));
    });

    it('should create a query with a string and non-string term', () => {
      const terms = [
        {
          term: 'name',
          operator: 'eq',
          value: { type: 'string', value: 'Fox' },
        }, {
          term: 'amount',
          operator: 'eq',
          value: { type: 'number', value: 1000 },
        }
      ];

      const query = toQuery(terms);

      const expectedQuery = {
        must: [{
          match: { name: 'Fox' },
        }],
      };

      const expectedFilter = {
        filter: [{
          term: { amount: 1000 },
        }],
      };

      const finalQuery = {
        query: {
          filtered: {
            ...composeQuery(expectedQuery),
            ...expectedFilter,
          },
        },
      };

      expect(query).to.eql(finalQuery);
    });

    it('should create a search with multiple terms and a sort order', () => {
      const terms = [
        {
          term: 'name',
          operator: 'eq',
          value: { type: 'string', value: 'Fox' },
        }, {
          term: 'amount',
          operator: 'eq',
          value: { type: 'number', value: 1000 },
        }
      ];

      const query = toQuery(terms, { sortBy: 'name' });

      const expectedQuery = {
        must: [{
          match: { name: 'Fox' },
        }],
      };

      const expectedFilter = {
        filter: [{
          term: { amount: 1000 },
        }],
      };

      const finalQuery = {
        query: {
          filtered: {
            ...composeQuery(expectedQuery),
            ...expectedFilter,
          },
        },
        sort: [{
          name: { order: 'asc' },
        }],
      };

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
      };

      expect(query).to.eql(composeQuery(expectedFilter));
    });

    it('should create a search with a nested query', () => {
      const terms = [{
        term: 'customer.name',
        operator: 'eq',
        value: { type: 'string', value: 'Adil Wali' },
      }];

      const query = toQuery(terms);

      const expectedQuery = {
        filter: [{
          nested: {
            path: 'customer',
            query: {
              bool: {
                filter: {
                  match: { 'customer.name': 'Adil Wali' },
                },
              },
            },
          },
        }],
      };

      expect(query).to.eql(composeQuery(expectedQuery));
    });
  });
});
