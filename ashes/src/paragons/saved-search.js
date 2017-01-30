/* @flow */

export type SavedSearch = {
  id: number,
  code: string,
  isSystem: bool,
  query: Array<Query>,
  rawQuery: { [key:string]: any },
  scope: string,
  storeAdminId: number,
  title: string,
  createdAt: string,
};

type Query = {
  display: string,
  operator: string,
  term: string,
  value: {
    type: string,
    value: string,
  },
};
