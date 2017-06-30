import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Users : ID',
    type: 'number',
    term: 'id'
  },
  {
    title: 'Users : Name',
    type: 'string',
    term: 'name'
  },
  {
    title: 'Users : email',
    type: 'string',
    term: 'email'
  },
  {
    title: 'Users : Date/Time Created',
    type: 'date',
    term: 'createdAt'
  },
];

const { reducer, actions } = makeLiveSearch(
  'users.list',
  searchTerms,
  'store_admins_search_view',
  'storeAdminsScope',
  {
    initialState: { sortBy: '-createdAt' },
  }
);

export {
  reducer as default,
  actions
};
