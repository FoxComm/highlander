import createImages from '../images';

const { actions, reducer } = createImages('product-variants');

export {
  actions,
  reducer as default
};
