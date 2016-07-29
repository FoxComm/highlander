/* @flow */

import React, { Element } from 'react';
import _ from 'lodash';

import CouponRow from './coupon-row';
import TableView from 'components/table/tableview';

import styles from './coupons-panel.css';

type Props = {
  coupons: Array<Object>,
};

const columns = [
  { field: 'name', text: 'Name' },
  { field: 'storefrontName', text: 'Storefront Name' },
  { field: 'code', text: 'Code' },
];

const CouponsPanel = (props: Props): Element => {
  const renderFn = (row: Object, index: number, isNew: boolean) => {
    return (
      <CouponRow
        key={`coupon-row-${row.id}`}
        item={row}
        columns={columns}
        onDelete={() => {}} />
    );
  }

  const { coupons } = props;
  if (_.isEmpty(coupons)) {
    return <div styleName="empty-message">No coupons applied.</div>;
  }

  const data = {
    rows: coupons,
  };

  return (
    <TableView
      columns={columns}
      data={data}
      emptyMessage="No coupons applied."
      renderRow={renderFn} />
  );
}

export default CouponsPanel;
