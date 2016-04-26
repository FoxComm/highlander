/**
 * @flow
 */

import React, { PropTypes, Element } from 'react';

import _ from 'lodash';
import { activeStatus } from '../../paragons/common';
import { transitionTo } from 'browserHistory';

import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

type CouponRowProps = {
  coupon: Object,
  columns: Array<string>,
  params: Object,
};

// This is a workaround for empty fields
const setCellContents = (coupon: Object, field: string) => {
  switch (field) {
    case 'totalUsed':
    case 'currentCarts':
      return _.get(coupon, field, 0);
    case 'state':
      return <RoundedPill text={activeStatus(coupon)} />;
    default:
      return _.get(coupon, field);
  }
};

const CouponRow = (props: CouponRowProps) => {
  const { coupon, columns, params } = props;
  const key = `coupon-${coupon.couponId}`;
  const clickAction = () => {
    transitionTo('coupon-details', { couponId: coupon.couponId });
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

CouponRow.propTypes = {
  coupon: PropTypes.object,
  columns: PropTypes.array,
  params: PropTypes.object,
};

export default CouponRow;
