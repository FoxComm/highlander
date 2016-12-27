/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as tracking from 'lib/analytics';

// components
import RadioButton from 'ui/radiobutton/radiobutton';
import Loader from 'ui/loader';
import CheckoutForm from '../checkout-form';

// types

import type { AsyncStatus } from 'types/async-actions';
import { saveShippingMethod } from 'modules/checkout';

// styles
import styles from './delivery.css';

type ShippingMethodLike = {
  id: number,
}

type Props = {
  onComplete: Function,
  shippingMethods: Array<any>,
  cart: Object,
  fetchShippingMethods: Function,
  shippingMethodCost: Function,
  saveShippingMethod: (shippingMethod: ShippingMethodLike) => Promise,
  isLoading: boolean,
  onUpdateCart: (cart: Object) => void,
  saveDeliveryState: AsyncStatus,
};

function mapStateToProps(state) {
  return {
    isLoading: _.get(state.asyncActions, ['shippingMethods', 'inProgress'], true),
    saveDeliveryState: _.get(state.asyncActions, 'saveShippingMethod', {}),
  };
}


class EditDelivery extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchShippingMethods();
  }

  @autobind
  handleSubmit() {
    const selectedMethod = this.props.cart.shippingMethod;
    if (selectedMethod) {
      tracking.chooseShippingMethod(selectedMethod.code || selectedMethod.name);
      this.props.saveShippingMethod(selectedMethod).then(this.props.onComplete);
    }
  }

  setShippingMethod(shippingMethod) {
    this.props.onUpdateCart({
      ...this.props.cart,
      shippingMethod,
    });
  }

  get shippingMethods() {
    const { shippingMethods, cart } = this.props;

    return shippingMethods.map(shippingMethod => {
      const cost = this.props.shippingMethodCost(shippingMethod.price);
      const checked = cart.shippingMethod && cart.shippingMethod.id == shippingMethod.id;

      return (
        <div key={shippingMethod.id} styleName="shipping-method">
          <RadioButton
            name="delivery"
            checked={checked || false}
            onChange={() => this.setShippingMethod(shippingMethod)}
            id={`delivery${shippingMethod.id}`}
          >
            {shippingMethod.name}
          </RadioButton>
          <div styleName="price">{cost}</div>
        </div>
      );
    });
  }

  render() {
    const { props } = this;

    if (props.isLoading) {
      return <Loader size="m" />;
    }

    return (
      <CheckoutForm
        submit={this.handleSubmit}
        title="DELIVERY METHOD"
        error={props.saveDeliveryState.err}
        inProgress={props.saveDeliveryState.inProgress}
      >
        {this.shippingMethods}
      </CheckoutForm>
    );
  }
}

export default connect(mapStateToProps, {
  saveShippingMethod,
})(EditDelivery);
