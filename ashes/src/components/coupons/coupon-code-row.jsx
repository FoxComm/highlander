/* @flow */

import React, { Element } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

type CouponCodeRowProps = {
  couponCode: Object,
  columns: Columns,
  params: Object,
};

const setCellContents = (coupon: Object, field: string) => {
  return _.get(coupon, field);
};

const CouponCodeRow = (props: CouponCodeRowProps) => {
  const { couponCode, columns, params } = props;
  const key = `coupon-code-${couponCode.code}`;

  return (
    <MultiSelectRow
      columns={columns}
      row={couponCode}
      setCellContents={setCellContents}
      params={params} />
  );
};

CouponCodeRow.propTypes = {
  couponCode: PropTypes.object,
  columns: PropTypes.array,
  params: PropTypes.object,
};

export default CouponCodeRow;
