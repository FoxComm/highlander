/* @flow */

import React, { Component, Element } from 'react';

import EditableContentBox from 'components/content-box/editable-content-box';
import PanelHeader from 'components/panel-header/panel-header';
import PaymentsPanel from 'components/payments-panel/payments-panel';

import type { Cart, PaymentMethod } from 'paragons/order';

type Props = {
  cart: Cart,
  status: string,
};

export default class Payments extends Component {
  props: Props;

  render(): Element {
    const { cart, status } = this.props;
    const { paymentMethods } = cart;

    const title = (
      <PanelHeader
        showStatus={true}
        status={status}
        text="Payment Method" />
    );

    const viewContent = <PaymentsPanel order={cart} paymentMethods={paymentMethods} />

    return (
      <EditableContentBox
        className="fc-order-payment"
        title={title}
        indentContent={false}
        viewContent={viewContent} />
    );
  }
}
