/**
 * @flow
 */

//libs
import React, { Element } from 'react';
import _ from 'lodash';
import styles from './form.css';

// helpers
import { isArchived } from 'paragons/common';

// components
import StatePill from '../object-page/state-pill';
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
      const codes = _.get(coupon, field) || [];
      if (codes.length > 1) {
        return (
          <span>{codes[0]} <span styleName="text-gray">+{codes.length - 1}</span></span>
        );
      }
      return codes[0];
    case 'state':
      return <StatePill object={coupon} />;
    default:
      return _.get(coupon, field);
  }
};

const CouponRow = (props: CouponRowProps) => {
  const { coupon, columns, params, promoId } = props;
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
      linkParams={{couponId: coupon.id, 'promotionId': promoId}}
    />
  );
};

export default CouponRow;
