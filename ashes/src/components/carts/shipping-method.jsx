/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import PanelHeader from 'components/panel-header/panel-header';
import ShippingMethodPanel from 'components/shipping/shipping-method';

import * as detailsActions from 'modules/carts/details';
import * as shippingActions from 'modules/carts/shipping-methods';

import type { Cart, ShippingMethod } from 'paragons/order';

function mapStateToProps(state) {
  return {
    shippingMethods: state.carts.shippingMethods,
  };
}

type Props = {
  cart: Cart,
  shippingMethods: {
    list: Array<ShippingMethod>,
  },
  status: string,
  fetchShippingMethods: Function,
  updateShippingMethod: Function,
};

type State = {
  isEditing: bool;
}

export class CartShippingMethod extends Component {
  props: Props;
  state: State = { isEditing: false };

  @autobind
  getShippingMethods() {
    const { referenceNumber } = this.props.cart;
    this.props.fetchShippingMethods(referenceNumber);
  }

  @autobind
  startEditing() {
    this.setState({ isEditing: true }, this.getShippingMethods);
  }

  @autobind
  completeEditing() {
    this.setState({ isEditing: false });
  }

  @autobind
  updateShippingMethod(cart: Cart, method: ShippingMethod) {
    this.props.updateShippingMethod(cart.referenceNumber, method.id);
  }

  render(): Element {
    const { cart, status } = this.props;
    const { shippingMethod } = cart;
    const { list } = this.props.shippingMethods;

    const title = <PanelHeader text="Shipping Method" showStatus={true} status={status} />;

    return (
      <ShippingMethodPanel
        currentOrder={cart}
        title={title}
        readOnly={false}
        availableShippingMethods={list}
        editAction={this.startEditing}
        isEditing={this.state.isEditing}
        editButtonId="shipping-method-edit-btn"
        doneAction={this.completeEditing}
        updateAction={this.updateShippingMethod}
        shippingMethods={[shippingMethod]} />
    );
  }
};

export default connect(mapStateToProps, { ...detailsActions, ...shippingActions })(CartShippingMethod);
