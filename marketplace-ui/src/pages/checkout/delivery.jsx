import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import localized from 'lib/i18n';

import Button from 'ui/buttons';
import Checkbox from 'ui/checkbox';
import EditableBlock from 'ui/editable-block';
import { Form } from 'ui/forms';
import Currency from 'ui/currency';
import Loader from 'ui/loader';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';

import type { CheckoutBlockProps } from './types';
import * as cartActions from 'modules/cart';
import { fetchShippingMethods } from 'modules/checkout';

const shippingMethodCost = (t, cost) => {
  return cost == 0
    ? <div styleName="delivery-cost">{t('FREE')}</div>
    : <Currency styleName="delivery-cost" value={cost}/>;
};

let ViewDelivery = (props) => {
  const { shippingMethod } = props;

  if (!shippingMethod) return <div></div>;

  return (
    <div styleName="shipping-method">
      <div>{shippingMethod.name}</div>
      {shippingMethodCost(props.t, shippingMethod.price)}
    </div>
  );
};
ViewDelivery = connect(state => state.cart)(localized(ViewDelivery));

function mapStateToProps(state) {
  return {
    shippingMethods: state.checkout.shippingMethods,
    selectedShippingMethod: state.cart.shippingMethod,
    isLoading: _.get(state.asyncActions, ['shippingMethods', 'inProgress'], true),
  };
}

/* ::`*/
@connect(mapStateToProps, {...cartActions, fetchShippingMethods})
@localized
/* ::`*/
class EditDelivery extends Component {

  componentWillMount() {
    this.props.fetchShippingMethods();
  }

  @autobind
  handleSubmit() {
    const { selectedShippingMethod: selectedMethod } = this.props;
    if (selectedMethod) {
      this.props.continueAction();
    }
  }

  get shippingMethods() {
    const { shippingMethods, selectedShippingMethod: selectedMethod, selectShippingMethod, t } = this.props;

    return shippingMethods.map(shippingMethod => {
      const cost = shippingMethodCost(t, shippingMethod.price);

      return (
        <div key={shippingMethod.id} styleName="shipping-method">
          <Checkbox
            name="delivery"
            checked={selectedMethod && selectedMethod.id == shippingMethod.id}
            onChange={() => selectShippingMethod(shippingMethod)}
            id={`delivery${shippingMethod.id}`}
          >
            {shippingMethod.name}
          </Checkbox>
          {cost}
        </div>
      );
    });
  }

  render() {
    const { isLoading, t } = this.props;

    if (isLoading) {
      return <Loader size="m" />;
    }

    return (
      <Form onSubmit={this.handleSubmit}>
        {this.shippingMethods}
        <ErrorAlerts error={this.props.error} />
        <Button isLoading={this.props.inProgress} styleName="checkout-submit" type="submit">{t('CONTINUE')}</Button>
      </Form>
    );
  }
}

const Delivery = (props: CheckoutBlockProps) => {
  const deliveryContent = (
    <div styleName="checkout-block-content">
      {props.isEditing ? <EditDelivery {...props} /> : <ViewDelivery />}
    </div>
  );

  const { t } = props;

  return (
    <EditableBlock
      {...props}
      styleName="checkout-block"
      title={t('DELIVERY')}
      content={deliveryContent}
    />
  );
};

export default localized(Delivery);
