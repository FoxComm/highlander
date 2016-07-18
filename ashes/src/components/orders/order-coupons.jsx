/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';

import ContentBox from 'components/content-box/content-box';
import PanelHeader from './panel-header';
import CouponRow from './discounts/coupon-row';
import TableView from 'components/table/tableview';

import styles from './styles/discounts.css';

type Target = {
  name: string,
  value: string|number|boolean,
};

type Props = {
  order: {
    referenceNumber: string,
  }
};

const viewColumns = [
  {field: 'name', text: 'Name'},
  {field: 'storefrontName', text: 'Storefront Name'},
  {field: 'code', text: 'Code'},
];

export default class OrderCoupons extends Component {
  props: Props;

  get orderReferenceNumber(): string {
    return this.props.order.referenceNumber;
  }

  get coupons(): Array<Object> {
    const coupon = _.get(this.props, 'order.coupon');

    if (!coupon) return [];

    return [coupon];
  }

  get viewContent(): Element {
    const coupons = this.coupons;
    if (_.isEmpty(coupons)) {
      return <div styleName="empty-message">No coupons applied.</div>;
    } else {
      return (
        <TableView
          columns={viewColumns}
          data={{rows: coupons}}
          emptyMessage="No coupons applied."
          renderRow={this.renderRow()}
        />
      );
    }
  }

  renderRow(): Function {
    const renderFn = (row: Object, index: number, isNew: boolean) => {
      return (
        <CouponRow
          key={`order-coupon-row-${row.id}`}
          item={row}
          columns={viewColumns}
          onDelete={_.noop}
        />
      );
    };
    return renderFn;
  }

  render(): Element {
    const title = <PanelHeader isOptional={true} text="Coupons" />;
    return (
      <ContentBox
        title={title}
        indentContent={false}
        viewContent={this.viewContent}
      />
    );
  }
}
