/**
 * @flow
 */

import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

const defaultContext = 'default';

const _archiveSku = createAsyncActions(
  'archiveSku',
  (code, context = defaultContext) => {
    return Api.delete(`/skus/${context}/${code}`);
  }
);

export const archiveSku = _archiveSku.perform;
