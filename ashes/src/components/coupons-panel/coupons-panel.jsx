/* @flow */

import React, { Element} from 'react';
import _ from 'lodash';

import CouponRow from './coupon-row';
import TableView from 'components/table/tableview';

import styles from './coupons-panel.css';

type Props = {
  coupons: Array<Object>;
  columns: Array<Object>;
  onDelete?: () => Promise<*>;
};

const CouponsPanel = (props: Props): Element<*>=> {
  const renderFn = (row: Object) => {
    return (
      <CouponRow
        key={`coupon-row-${row.id}`}
        item={row}
        columns={props.columns}
        onDelete={props.onDelete} />
    );
  };

  const { coupons } = props;
  if (_.isEmpty(coupons)) {
    return <div styleName="empty-message">No coupons applied.</div>;
  }

  const data = {
    rows: coupons,
  };

  return (
    <TableView
      styleName="coupons-panel"
      columns={props.columns}
      data={data}
      emptyMessage="No coupons applied."
      renderRow={renderFn}
    />
  );
};

export default CouponsPanel;
