import makePagination from '../pagination';

const { reducer, fetch, updateStateAndFetch } = makePagination('/rmas', 'RMAS');

export {
  reducer as default,
  fetch as fetchRmas,
  updateStateAndFetch
};
