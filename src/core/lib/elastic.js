/* @flow */
import { assoc } from 'sprout-data';


export type TermFilter = {
  term: {
    [key:string]: any,
  },
};

export type MatchFilter = {
  match: {
    _all: {
      query: string,
      type: string,
      max_expansions: number,
    },
  },
};

export type BoolQuery = {
  query: {
    bool: {
      // filter clauses are like must clauses except that they do not contribute to the score
      filter: Array<TermFilter>,
      must?: Array<MatchFilter | TermFilter>,
      must_not?: Array<MatchFilter | TermFilter>,
    },
  },
};

export function termFilter(term: string, value: any): TermFilter {
  return {
    term: {
      [term]: value,
    },
  };
}

export function defaultSearch(context: string): BoolQuery {
  return {
    query: {
      bool: {
        filter: [termFilter('context', context)],
      },
    },
  };
}

export function addTermFilter(initialQuery: BoolQuery, term: TermFilter): BoolQuery {
  return assoc(initialQuery,
    ['query', 'bool', 'filter'], [...initialQuery.query.bool.filter, term]
  );
}

export function addMustFilter(initialQuery: BoolQuery, filter: MatchFilter | TermFilter): BoolQuery {
  return assoc(initialQuery,
    ['query', 'bool', 'must'], [...initialQuery.query.bool.must || [], filter]
  );
}

export function addMustNotFilter(initialQuery: BoolQuery, filter: MatchFilter | TermFilter): BoolQuery {
  return assoc(initialQuery,
    ['query', 'bool', 'must_not'], [...initialQuery.query.bool.must_not || [], filter]
  );
}

export function addMatchQuery(query: BoolQuery, searchString: string): BoolQuery {
  const matchFilter = {
    match: {
      _all: {
        query: searchString,
        type: 'phrase_prefix',
        max_expansions: 3,
      },
    },
  };

  return addMustFilter(query, matchFilter);
}
