import makePagination from '../pagination';


const {reducer, fetch, actionAddEntity} = makePagination('/groups', 'GROUPS');

export {
  reducer as default,
  fetch,
  actionAddEntity as addGroup
};
