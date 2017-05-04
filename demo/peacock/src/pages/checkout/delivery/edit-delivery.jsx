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
  saveDeliveryState: AsyncStatus,
  toggleDeliveryModal: Function,
};

type State = {
  shippingMethod: Object,
};

class EditDelivery extends Component {
  props: Props;

  state: State = {
    shippingMethod: {},
  };

  componentWillMount() {
    this.props.fetchShippingMethods();
    if (!_.isEmpty(this.props.cart.shippingMethod)) {
      this.setState({ shippingMethod: this.props.cart.shippingMethod });
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.cart.shippingAddress !== this.props.cart.shippingAddress) {
      this.props.fetchShippingMethods();
    }
  }

  @autobind
  handleSubmit() {
    const selectedMethod = this.state.shippingMethod;
    if (selectedMethod) {
      tracking.chooseShippingMethod(selectedMethod.code || selectedMethod.name);
      this.props.saveShippingMethod(selectedMethod).then(this.props.onComplete);
    }
  }

  setShippingMethod(shippingMethod) {
    this.setState({ shippingMethod });
  }

  get shippingMethods() {
    const { shippingMethods, cart } = this.props;

    return shippingMethods.map((shippingMethod) => {
      const cost = this.props.shippingMethodCost(shippingMethod.price);
      const checked = cart.shippingMethod && this.state.shippingMethod.id == shippingMethod.id;
      const methodClasses = classNames(styles['shipping-method'], {
        [styles.chosen]: checked,
      });
      return (
        <div key={shippingMethod.id} className={methodClasses} onClick={() => this.setShippingMethod(shippingMethod)}>
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
        title="Shipping methods"
        error={props.saveDeliveryState.err}
        inProgress={props.saveDeliveryState.inProgress}
        action={action}
        buttonDisabled={_.isEmpty(this.state.shippingMethod)}
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
