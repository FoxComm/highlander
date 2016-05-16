import createImages from '../images';

const { actions, reducer } = createImages('products');

export {
  actions,
  reducer as default
};
