/**
 * @flow
 */

import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

const defaultContext = 'default';

const _archivePromotion = createAsyncActions(
  'archivePromotion',
  (id, context = defaultContext) => {
    return Api.delete(`/promotions/${context}/${id}`);
  }
);

export const archivePromotion = _archivePromotion.perform;
