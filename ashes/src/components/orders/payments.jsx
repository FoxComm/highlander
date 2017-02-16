/* @flow */

import React, { Element} from 'react';

import ContentBox from 'components/content-box/content-box';
import PanelHeader from 'components/panel-header/panel-header';
import PaymentsPanel from 'components/payments-panel/payments-panel';

import type { Order, PaymentMethod } from 'paragons/order';

type Props = {
  details: {
    order: Order,
  },
};

export default class Payments extends React.Component {
  props: Props;

  render() {
    const { order } = this.props.details;
    const { paymentMethods } = order;

    const title = <PanelHeader showStatus={false} text="Payment Method" />;

    return (
      <ContentBox
        className="fc-order-payment"
        title={title}
        indentContent={false}>
        <PaymentsPanel order={order} paymentMethods={paymentMethods} />
      </ContentBox>
    );
  }
}
