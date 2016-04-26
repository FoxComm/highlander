import createImages from '../images';

const { actions, reducer } = createImages('images', 'products');

export {
  actions,
  reducer as default
};
