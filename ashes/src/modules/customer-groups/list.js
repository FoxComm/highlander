import makePagination from '../pagination';

const {reducer, fetch, addEntity, updateStateAndFetch} = makePagination('/groups', 'customerGroups.list');

export {
  reducer as default,
  fetch,
  addEntity as addGroup,
  updateStateAndFetch
};
