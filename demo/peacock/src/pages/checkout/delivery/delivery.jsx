/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';

// components
import Currency from 'ui/currency';
import EditDelivery from './edit-delivery';
import ViewDelivery from './view-delivery';
import ActionLink from 'ui/action-link/action-link';

// styles
import styles from './delivery.css';

// types
import type { CheckoutBlockProps } from '../types';

class Delivery extends Component {
  props: CheckoutBlockProps;

  @autobind
  shippingMethodCost(cost) {
    const { t } = this.props;

    return cost == 0
      ? <div styleName="delivery-cost">{t('FREE')}</div>
      : <Currency styleName="delivery-cost" value={cost} />;
  }


  get action() {
    return (
      <ActionLink
        action={this.props.toggleModal}
        title='Choose'
        styleName="action-link-delivery"
      />
    );
  }

  get content() {
    if (this.props.isEditing) {
      return (
        <EditDelivery {...this.props} shippingMethodCost={this.shippingMethodCost} />
      );
    }

    return (
      <ViewDelivery
        shippingMethodCost={this.shippingMethodCost}
        shippingMethod={this.props.shippingMethod}
      />
    );
  }

  render() {
    const { t } = this.props;

    return (
      <div>
        <div styleName="header">
          <span styleName="title">Delivery</span>
          {this.action}
        </div>
        {this.content}
      </div>
    );
  }
}

export default localized(Delivery);
