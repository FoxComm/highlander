import makePagination from '../pagination';


const {reducer, fetch, addEntity, updateStateAndFetch} = makePagination('/groups', 'GROUPS');

export {
  reducer as default,
  fetch,
  addEntity as addGroup,
  updateStateAndFetch
};
