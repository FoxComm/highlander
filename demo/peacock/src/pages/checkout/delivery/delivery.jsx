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
import Loader from 'ui/loader';

// actions
import { toggleDeliveryModal } from 'modules/checkout.js';

// types
import type { CheckoutBlockProps } from '../types';
import type { AsyncStatus } from 'types/async-actions';

import styles from './delivery.css';

type Props = CheckoutBlockProps & {
  cartState: AsyncStatus,
  toggleDeliveryModal: () => void,
  deliveryModalVisible: boolean,
  shippingMethod: Object,
  shippingAddressEmpty: boolean,
  loadingShippingMethods: boolean,
};

class Delivery extends Component {
  props: Props;

  @autobind
  shippingMethodCost(cost) {
    const { t } = this.props;

    return cost == 0
      ? <div styleName="delivery-cost">{t('Free')}</div>
      : <Currency styleName="delivery-cost" value={cost} />;
  }


  get action() {
    const { props } = this;
    const methodsLoadedAndEmpty = _.isEmpty(props.shippingMethods) && !props.loadingShippingMethods;

    if (props.shippingAddressEmpty || methodsLoadedAndEmpty) return null;

    return (
      <ActionLink
        action={this.props.toggleDeliveryModal}
        title="Choose"
        styleName="action-link-delivery"
      />
    );
  }

  get content() {
    const { props } = this;

    if (props.cartState.finished) {
      return (
        <div styleName="content">
          <ViewDelivery
            shippingMethodCost={this.shippingMethodCost}
            shippingMethod={props.shippingMethod}
            shippingAddressEmpty={props.shippingAddressEmpty}
            shippingMethodsEmpty={_.isEmpty(props.shippingMethods)}
            loadingShippingMethods={props.loadingShippingMethods}
          />
          <Modal
            show={props.deliveryModalVisible}
            toggle={props.toggleDeliveryModal}
          >
            <EditDelivery {...this.props} shippingMethodCost={this.shippingMethodCost} />
          </Modal>
        </div>
      );
    }

    return (
      <Loader size="m" />
    );
  }

  render() {
    return (
      <div>
        <div styleName="header">
          <span styleName="title">Shipping methods</span>
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
    cartState: _.get(state.asyncActions, 'cart', false),
    shippingMethod: _.get(state.cart, 'shippingMethod', {}),
    loadingShippingMethods: _.get(state.asyncActions, ['shippingMethods', 'inProgress'], false),
  };
};

export default _.flowRight(
  localized,
  connect(mapStateToProps, {
    toggleDeliveryModal,
  })
)(Delivery);
