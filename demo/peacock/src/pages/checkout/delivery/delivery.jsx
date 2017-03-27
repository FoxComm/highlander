/* @flow */

import React, { Component } from 'react';

// libs
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import _ from 'lodash';
import { connect } from 'react-redux';

// components
import Currency from 'ui/currency';
import EditDelivery from './edit-delivery';
import ViewDelivery from './view-delivery';
import ActionLink from 'ui/action-link/action-link';
import Modal from 'ui/modal/modal';

// actions
import { toggleDeliveryModal } from 'modules/checkout.js';

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
        action={this.props.toggleDeliveryModal}
        title='Choose'
        styleName="action-link-delivery"
      />
    );
  }

  get content() {
    const { deliveryModalVisible, toggleDeliveryModal } = this.props;
    return (
      <div>
        <ViewDelivery
          shippingMethodCost={this.shippingMethodCost}
          shippingMethod={this.props.shippingMethod}
        />
        <Modal
          show={deliveryModalVisible}
          toggle={toggleDeliveryModal}
        >
          <EditDelivery {...this.props} shippingMethodCost={this.shippingMethodCost} />
        </Modal>
      </div>
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

const mapStateToProps = (state) => {
  return {
    deliveryModalVisible: _.get(state.checkout, 'deliveryModalVisible', false),
  };
};

export default _.flowRight(
  localized,
  connect(mapStateToProps, {
    toggleDeliveryModal,
  })
)(Delivery);
