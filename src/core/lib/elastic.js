/* @flow */
import { assoc } from 'sprout-data';


export type TermFilter = {
  term: {
    [key:string]: any,
  },
};

export type MatchQuery = {
  match: {
    _all: {
      query: string,
      type: string,
      max_expansions: number,
    },
  },
};

export type BoolFilter = {
  query: {
    bool: {
      filter: Array<TermFilter>,
      must?: ?Array<MatchQuery | TermFilter>,
      must_not?: ?Array<MatchQuery | TermFilter>,
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

export function defaultSearch(context: string): BoolFilter {
  return {
    query: {
      bool: {
        filter: [termFilter('context', context)],
      },
    },
  };
}

export function addTermFilter(initialFilter: BoolFilter, term?: ?TermFilter, mustFilters: any = {}): BoolFilter {
  let filter = initialFilter;
  const { must, mustNot } = mustFilters;
  const existingFilters = filter.query.bool.filter;

  if (term) {
    filter = assoc(filter, ['query', 'bool', 'filter'], [...existingFilters, term]);
  }

  if (must) {
    filter = assoc(filter, ['query', 'bool', 'must'], [...filter.query.bool.must || [], must]);
  }

  if (mustNot) {
    filter = assoc(filter, ['query', 'bool', 'must_not'], [...filter.query.bool.must_not || [], mustNot]);
  }

  return filter;
}

export function addMatchQuery(filter: BoolFilter, searchString: string): BoolFilter {
  const matchQuery = {
    match: {
      _all: {
        query: searchString,
        type: 'phrase_prefix',
        max_expansions: 3,
      },
    },
  };

  const matchQueries = filter.query.bool.must || [];
  return assoc(filter, ['query', 'bool', 'must'], [...matchQueries, matchQuery]);
}
