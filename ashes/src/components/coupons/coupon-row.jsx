/* @flow */

//libs
import React, { Element } from 'react';
import _ from 'lodash';
import styles from './form.css';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
import { RoundedPill } from 'components/core/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';

type CouponRowProps = {
  coupon: Object,
  columns: Columns,
  params: Object,
  promotionId: Number
};

// This is a workaround for empty fields
const setCellContents = (coupon: Object, field: string) => {
  const codes = _.get(coupon, field) || [];

  switch (field) {
    case 'totalUsed':
    case 'currentCarts':
      return _.get(coupon, field, 0);
    case 'codes':
      if (codes.length > 1) {
        return (
          <span>{codes[0]} <span styleName="text-gray">+{codes.length - 1}</span></span>
        );
      }
      return codes[0];
    case 'state':
      return <RoundedPill text={activeStatus(coupon)} />;
    case 'maxUsesPerCode':
    case 'maxUsesPerCustomer':
      return _.get(coupon, field) || <span>Unlimited</span>;
    default:
      return _.get(coupon, field);
  }
};

const CouponRow = (props: CouponRowProps) => {
  const { coupon, columns, params, promotionId } = props;
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
      linkParams={{couponId: coupon.id, promotionId: promotionId}}
    />
  );
};

export default CouponRow;
