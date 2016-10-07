/**
 * @flow
 */

import createAsyncActions from '../async-utils';
import Api from '../../lib/api';

const defaultContext = 'default';

const _archiveCoupon = createAsyncActions(
  'archiveCoupon',
  (id, context = defaultContext) => {
    return Api.delete(`/coupons/${context}/${id}`);
  }
);

export const archiveCoupon = _archiveCoupon.perform;
