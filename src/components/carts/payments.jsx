/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { trackEvent } from 'lib/analytics';

import { AddButton } from 'components/common/buttons';
import EditableContentBox from 'components/content-box/editable-content-box';
import PanelHeader from 'components/panel-header/panel-header';
import PaymentsPanel from 'components/payments-panel/payments-panel';

import type { Cart, PaymentMethod } from 'paragons/order';

type Props = {
  cart: Cart,
  status: string,
};

type State = {
  isAdding: boolean,
  isEditing: boolean,
};

export default class Payments extends Component {
  props: Props;
  state: State = {
    isAdding: false,
    isEditing: false,
  };

  @autobind
  toggleEdit() {
    this.setState({ isEditing: !this.state.isEditing });
  }

  get editingActions() {
    if (!this.state.isAdding) {
      const handleClick = () => {
        trackEvent('Orders', 'add_new_payment_method');
        this.setState({ isAdding: true });
      };
      return <AddButton onClick={handleClick} />;
    }
  }


  render(): Element {
    const { cart, status } = this.props;
    const { paymentMethods } = cart;

    const title = (
      <PanelHeader
        showStatus={true}
        status={status}
        text="Payment Method" />
    );

    const content = (
      <PaymentsPanel
        isAdding={this.state.isAdding}
        isEditing={this.state.isEditing}
        order={cart}
        paymentMethods={paymentMethods} />
    );

    return (
      <EditableContentBox
        className="fc-order-payment"
        title={title}
        indentContent={false}
        isEditing={this.state.isEditing}
        editAction={this.toggleEdit}
        editingActions={this.editingActions}
        doneAction={this.toggleEdit}
        editContent={content}
        viewContent={content} />
    );
  }
}
