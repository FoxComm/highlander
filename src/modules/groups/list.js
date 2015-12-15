import makePagination from '../pagination';

const {reducer, fetch} = makePagination('/groups', 'GROUPS');

export default reducer;
export {
  fetch
};
