/* @flow */

import React, { Component } from 'react';

import ContentBox from 'components/content-box/content-box';
import CouponsPanel from 'components/coupons-panel/coupons-panel';
import PanelHeader from 'components/panel-header/panel-header';

type Props = {
  coupon: ?Object,
};

const columns = [
  { field: 'name', text: 'Name' },
  { field: 'storefrontName', text: 'Storefront Name' },
  { field: 'code', text: 'Code' },
];

export default class OrderCoupons extends Component {
  props: Props;

  render() {
    const title = <PanelHeader showStatus={false} isOptional={true} text="Coupons" />;
    const coupons = this.props.coupon ? [this.props.coupon] : [];
    const content = <CouponsPanel coupons={coupons} columns={columns} />;

    return (
      <ContentBox
        title={title}
        indentContent={false}
        viewContent={content}
      />
    );
  }
}
