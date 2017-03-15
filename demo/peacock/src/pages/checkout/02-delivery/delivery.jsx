/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';

// components
import EditableBlock from 'ui/editable-block';
import Currency from 'ui/currency';
import EditDelivery from './edit-delivery';
import ViewDelivery from './view-delivery';

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
      : <Currency styleName="delivery-cost" value={cost}/>;
  }

  @autobind
  renderContent() {
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
      <EditableBlock
        isEditing={this.props.isEditing}
        editAction={this.props.editAction}
        editAllowed={this.props.editAllowed}
        styleName="delivery"
        title={t('DELIVERY')}
        content={this.renderContent()}
      />
    );
  }
}

export default localized(Delivery);
