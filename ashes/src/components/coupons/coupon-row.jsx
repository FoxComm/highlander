/**
 * @flow
 */

//libs
import React, { Element } from 'react';
import _ from 'lodash';
import styles from './form.css';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
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
    case 'codes':
      const codes = _.get(coupon, field, []);
      const firstCode = codes[0];
      return coupon.codes.length === 1 ? firstCode :
        <span>{firstCode} <span styleName="text-gray">+{codes.length - 1}</span></span>;
    case 'state':
      return <RoundedPill text={activeStatus(coupon)} />;
    default:
      return _.get(coupon, field);
  }
}

const CouponRow = (props: CouponRowProps) => {
  const { coupon, columns, params } = props;
  const key = `coupon-${coupon.id}`;
  const commonParams = {
    columns,
    row: coupon,
    setCellContents,
    params,
  };

  if (isArchived(coupon)) {
    return <MultiSelectRow {...commonParams} />;
  }

  return (
    <MultiSelectRow
      {...commonParams}
      linkTo="coupon-details"
      linkParams={{couponId: coupon.id}}
    />
  );
};

export default CouponRow;
