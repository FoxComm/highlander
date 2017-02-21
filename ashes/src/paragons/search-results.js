/* @flow */

export type SearchResult<T> = {
  result: Array<T>,
  pagination: {
    total: number,
  },
};

