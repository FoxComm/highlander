import makePagination from '../pagination';

const { reducer, fetch } = makePagination('/rmas', 'RMAS');

export {
  reducer as default,
  fetch as fetchRmas
};
