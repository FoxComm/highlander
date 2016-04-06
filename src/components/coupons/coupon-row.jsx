/**
 * @flow
 */

import React, { PropTypes, Element } from 'react';

import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

import { DateTime } from '../common/datetime';
import { Checkbox } from '../checkbox/checkbox';
import Currency from '../common/currency';
import Link from '../link/link';
import MultiSelectRow from '../table/multi-select-row';

type CouponRowProps = {
  coupon: Object,
  columns: Array<string>,
  params: Object,
};

type CouponContext = {
  history: Object,
};

const setCellContents = (coupon: Object, field: string) => {
  return _.get(coupon, field);
};

const CouponRow = (props: CouponRowProps, context: CouponContext) => {
  const { coupon, columns, params } = props;
  const key = `coupon-${coupon.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'coupon', { coupon: coupon.id });
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
