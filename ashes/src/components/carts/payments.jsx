/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { trackEvent } from 'lib/analytics';
import _ from 'lodash';

import { AddButton } from 'components/core/button';
import EditableContentBox from 'components/content-box/editable-content-box';
import PanelHeader from 'components/panel-header/panel-header';
import PaymentsPanel from 'components/payments-panel/payments-panel';

import type { Cart } from 'paragons/order';

type Props = {
  cart: Cart,
  id: string,
  status: string,
  creditCardSelected: boolean,
  giftCardAdded: boolean,
  giftCardUpdated: boolean,
  storeCreditSet: boolean,
};

type State = {
  isAdding: boolean,
  isEditing: boolean,
};

export class Payments extends Component {
  props: Props;
  state: State = {
    isAdding: false,
    isEditing: false,
  };

  componentWillReceiveProps(nextProps: Props) {
    const ccUpdated = !this.props.creditCardSelected && nextProps.creditCardSelected;
    const scUpdated = !this.props.storeCreditSet && nextProps.storeCreditSet;
    const gcAdded = !this.props.giftCardAdded && nextProps.giftCardAdded;
    const gcUpdated = !this.props.giftCardUpdated && nextProps.giftCardUpdated;

    if (ccUpdated || scUpdated || gcAdded || gcUpdated) {
      this.setState({ isAdding: false });
    }
  }

  @autobind
  startEdit() {
    this.setState({ isEditing: true });
  }

  @autobind
  completeEdit() {
    this.setState({
      isAdding: false,
      isEditing: false,
    });
  }

  @autobind
  cancelAdding() {
    this.setState({
      isAdding: false,
    });
  }

  get editingActions() {
    if (!this.state.isAdding) {
      const handleClick = () => {
        trackEvent('Orders', 'add_new_payment_method');
        this.setState({ isAdding: true });
      };
      return <AddButton id="fct-add-btn__payment-method" onClick={handleClick} />;
    }
  }


  render() {
    const { cart, id, status } = this.props;
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
        cancelAdding={this.cancelAdding}
        order={cart}
        paymentMethods={paymentMethods}
        doneButtonId="payment-method-done-btn"
      />
    );

    return (
      <EditableContentBox
        className="fc-order-payment"
        id={id}
        title={title}
        indentContent={false}
        isEditing={this.state.isEditing}
        editAction={this.startEdit}
        editButtonId="fct-edit-btn__payment-method"
        doneButtonId="fct-done-btn__payment-method"
        editingActions={this.editingActions}
        doneAction={this.completeEdit}
        editContent={content}
        viewContent={content} />
    );
  }
}

function mapStateToProps(state) {
  return {
    creditCardSelected: _.get(state.asyncActions, 'selectCreditCard.finished', false),
    giftCardAdded: _.get(state.asyncActions, 'addGiftCardPayment.finished', false),
    giftCardUpdated: _.get(state.asyncActions, 'editGiftCardPayment.finished', false),
    storeCreditSet: _.get(state.asyncActions, 'setStoreCreditPayment.finished', false),
  };
}

export default connect(mapStateToProps, null)(Payments);
