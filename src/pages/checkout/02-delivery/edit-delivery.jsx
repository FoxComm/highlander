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

// styles
import styles from './delivery.css';


type Props = {
  continueAction: Function,
  shippingMethods: Array<any>,
  cart: Object,
  fetchShippingMethods: Function,
  shippingMethodCost: Function,
  isLoading: boolean,
  error: ?Array<any>,
  onUpdateCart: (cart: Object) => void,
};

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
      this.props.continueAction();
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
    const { isLoading } = this.props;

    if (isLoading) {
      return <Loader size="m" />;
    }

    return (
      <CheckoutForm
        submit={this.handleSubmit}
        title="DELIVERY METHOD"
        error={this.props.error}
      >
        {this.shippingMethods}
      </CheckoutForm>
    );
  }
}

function mapStateToProps(state) {
  return {
    isLoading: _.get(state.asyncActions, ['shippingMethods', 'inProgress'], true),
  };
}

export default connect(mapStateToProps)(EditDelivery);
