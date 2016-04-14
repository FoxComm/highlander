
/* @flow */

import React, { PropTypes, Element } from 'react';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

type CouponCodeRowProps = {
  coupon: Object,
  columns: Array<string>,
  params: Object,
};

const setCellContents = (coupon: Object, field: string) => {
  return _.get(coupon, field);
};

const CouponCodeRow = (props: CouponCodeRowProps) => {
  const { coupon, columns, params } = props;
  const key = `coupon-code-${coupon.code}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      row={coupon}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default CouponCodeRow;
