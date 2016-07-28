/* @flow */

import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import PanelHeader from 'components/panel-header/panel-header';
import ShippingMethod from 'components/shipping/shipping-method';

import * as detailsActions from 'modules/carts/details';
import * as shippingActions from 'modules/carts/shipping-methods';

function mapStateToProps(state) {
  return {
    shippingMethods: state.carts.shippingMethods,
  };
}

type Props = {
  cart: {
    referenceNumber: string,
    shippingMethod: Object,
  },
  shippingMethods: {
    list: Array<Object>,
  },
  status: string,
};

type State = {
  isEditing: bool;
}

@connect(mapStateToProps, { ...detailsActions, ...shippingActions })
export default class CartShippingMethod extends Component {
  props: Props;
  state: State = { isEditing: false };

  @autobind
  getShippingMethods() {
    const { referenceNumber } = this.props.cart;
    this.props.fetchShippingMethods(referenceNumber);
  }

  @autobind
  startEditing() {
    this.setState({ isEditing: true }, this.getShippingMethods)
  }

  @autobind
  completeEditing() {
    this.setState({ isEditing: false });
  }

  @autobind
  updateShippingMethod(order, method) {
    this.props.updateShippingMethod(order.referenceNumber, method.id);
  }

  render(): Element {
    const { cart, status } = this.props;
    const { shippingMethod } = cart;
    const { list } = this.props.shippingMethods;

    const title = <PanelHeader text="Shipping Method" showStatus={true} status={status} />;

    return (
      <ShippingMethod
        currentOrder={cart}
        title={title}
        readOnly={false}
        availableShippingMethods={list}
        editAction={this.startEditing}
        isEditing={this.state.isEditing}
        doneAction={this.completeEditing}
        updateAction={this.updateShippingMethod}
        shippingMethods={[shippingMethod]} />
    );
  }
};
