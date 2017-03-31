/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as tracking from 'lib/analytics';
import classNames from 'classnames';

// components
import RadioButton from 'ui/radiobutton/radiobutton';
import Loader from 'ui/loader';
import CheckoutForm from 'pages/checkout/checkout-form';

import { saveShippingMethod } from 'modules/checkout';

import type { AsyncStatus } from 'types/async-actions';

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
  saveShippingMethod: (shippingMethod: ShippingMethodLike) => Promise<*>,
  isLoading: boolean,
  onUpdateCart: (cart: Object) => void,
  saveDeliveryState: AsyncStatus,
  toggleDeliveryModal: Function,
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

    return shippingMethods.map((shippingMethod) => {
      const cost = this.props.shippingMethodCost(shippingMethod.price);
      const checked = cart.shippingMethod && cart.shippingMethod.id == shippingMethod.id;
      const methodClasses = classNames(styles['shipping-method'], {
        [styles.chosen]: checked,
      });
      return (
        <div key={shippingMethod.id} className={methodClasses}>
          <RadioButton
            name="delivery"
            checked={checked || false}
            onChange={() => this.setShippingMethod(shippingMethod)}
            id={`delivery${shippingMethod.id}`}
          >
            <div className={styles['method-name']}>{shippingMethod.name}</div>
          </RadioButton>
          <div className={styles['method-price']}>{cost}</div>
        </div>
      );
    });
  }

  render() {
    const { props } = this;

    if (props.isLoading) {
      return <Loader size="m" />;
    }

    const action = {
      title: 'Close',
      handler: this.props.toggleDeliveryModal,
    };

    return (
      <CheckoutForm
        buttonLabel="Apply"
        submit={this.handleSubmit}
        title="Delivery"
        error={props.saveDeliveryState.err}
        inProgress={props.saveDeliveryState.inProgress}
        action={action}
      >
        <div className={styles['shipping-methods']}>
          {this.shippingMethods}
        </div>
      </CheckoutForm>
    );
  }
}

function mapStateToProps(state) {
  return {
    isLoading: _.get(state.asyncActions, ['shippingMethods', 'inProgress'], true),
    saveDeliveryState: _.get(state.asyncActions, 'saveShippingMethod', {}),
  };
}

export default connect(mapStateToProps, {
  saveShippingMethod,
})(EditDelivery);
