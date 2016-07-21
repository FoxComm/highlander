/**
 * @flow
 */

import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

const defaultContext = 'default';

const _archiveProduct = createAsyncActions(
  'archiveProduct',
  (id, context = defaultContext) => {
    return Api.delete(`/products/${context}/${id}/archive`);
  }
);

export const archiveProduct = _archiveProduct.perform;
