import createImages from '../images';

const { actions, reducer } = createImages('skus');

export {
  actions,
  reducer as default
};
