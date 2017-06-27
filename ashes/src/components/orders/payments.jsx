/* @flow */

import React, { Element } from 'react';

import ContentBox from 'components/content-box/content-box';
import PanelHeader from 'components/panel-header/panel-header';
import PaymentsPanel from 'components/payments-panel/payments-panel';

import OrderParagon from 'paragons/order';

type Props = {
  details: {
    order: OrderParagon,
  },
};

export default class Payments extends React.Component {
  props: Props;

  render() {
    const { order } = this.props.details;
    const { paymentMethods } = order;

    return (
      <ContentBox className="fc-order-payment" title="Payment Method" indentContent={false}>
        <PaymentsPanel order={order} paymentMethods={paymentMethods} />
      </ContentBox>
    );
  }
}
