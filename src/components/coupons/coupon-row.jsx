/**
 * @flow
 */

import React, { PropTypes, Element } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import MultiSelectRow from '../table/multi-select-row';

type CouponRowProps = {
  coupon: Object,
  columns: Array<string>,
  params: Object,
};

type CouponContext = {
  history: Object,
};

// This is a workaround for empty fields
const setCellContents = (coupon: Object, field: string) => {
  switch (field) {
    case 'totalUsed':
    case 'currentCarts':
      return _.get(coupon, field, 0);
    default:
      return _.get(coupon, field);
  }
};

const CouponRow = (props: CouponRowProps, context: CouponContext) => {
  const { coupon, columns, params } = props;
  const key = `coupon-${coupon.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'coupon-details', { couponId: coupon.id });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={coupon}
      setCellContents={setCellContents}
      params={params} />
  );
};

CouponRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default CouponRow;
