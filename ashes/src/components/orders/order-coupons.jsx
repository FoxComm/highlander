/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';

import ContentBox from 'components/content-box/content-box';
import CouponsPanel from 'components/coupons-panel/coupons-panel';
import PanelHeader from 'components/panel-header/panel-header';

type Props = {
  order: {
    coupon: Object,
  },
};

const columns = [
  { field: 'name', text: 'Name' },
  { field: 'storefrontName', text: 'Storefront Name' },
  { field: 'code', text: 'Code' },
];

export default class OrderCoupons extends Component {
  props: Props;

  get coupons(): Array<Object> {
    const coupon = _.get(this.props, 'order.coupon');

    if (!coupon) return [];

    return [coupon];
  }

  render() {
    const title = <PanelHeader showStatus={false} isOptional={true} text="Coupons" />;
    const content = <CouponsPanel coupons={this.coupons} columns={columns} />;
    return (
      <ContentBox
        title={title}
        indentContent={false}
        viewContent={content}
      />
    );
  }
}
