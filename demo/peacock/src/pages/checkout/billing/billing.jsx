/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import localized from 'lib/i18n';

// components
import EditBilling from './edit-billing';
import ViewBilling from './view-billing';
import PromoCode from 'components/promo-code/promo-code';
import ActionLink from 'ui/action-link/action-link';
import Modal from 'ui/modal/modal';
import Loader from 'ui/loader';

import { togglePaymentModal } from 'modules/checkout';

import type { CheckoutBlockProps, BillingData } from '../types';

import styles from './billing.css';

type Props = CheckoutBlockProps & {
  paymentMethods: Array<any>,
  creditCard: BillingData,
  paymentModalVisible: boolean,
};

class Billing extends Component {
  props: Props;

  get giftCards() {
    return _.filter(this.props.paymentMethods, {type: 'giftCard'});
  }

  get editBilling() {
    return (
      <EditBilling
        {...this.props}
        giftCards={this.giftCards}
      />
    );
  }

  get action() {
    const { props } = this;
    let title;
    let icon;

    if (props.cartState.finished) {
      if (props.creditCard) {
        title = 'Choose';
      } else {
        title = 'Add new';
        icon = {
          name: 'fc-plus',
          className: styles.plus,
        };
      }

      return (
        <ActionLink
          action={this.props.togglePaymentModal}
          title={title}
          styleName="action-link-payment"
          icon={icon}
        />
      );
    }
  }

  get content() {
    const { coupon, promotion, totals, creditCard, paymentModalVisible } = this.props;

    return (
      <div styleName="billing-summary">
        <ViewBilling billingData={creditCard} />

        {coupon &&
          <div styleName="promo-line">
            <PromoCode
              placeholder="Coupon Code"
              coupon={coupon}
              promotion={promotion}
              discountValue={totals.adjustments}
              allowDelete={false}
              editable={false}
              context="billingView"
            />
          </div>}

        {this.giftCards &&
          <div styleName="promo-line">
            <PromoCode
              placeholder="Gift Card Number"
              giftCards={this.giftCards}
              allowDelete={false}
              editable={false}
              context="billingView"
            />
          </div>}
          <Modal
            show={paymentModalVisible}
            toggle={this.props.togglePaymentModal}
          >
            {this.editBilling}
          </Modal>
      </div>
    );
  }

  render() {
    return (
      <div>
        <div styleName="header">
          <span styleName="title">Payment</span>
          {this.action}
        </div>
        {this.content}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    creditCard: state.checkout.creditCard,
    ...state.cart,
    cartState: _.get(state.asyncActions, 'cart', {}),
    paymentModalVisible: _.get(state.checkout, 'paymentModalVisible', false),
  };
};

export default connect(mapStateToProps, {
  togglePaymentModal,
})(localized(Billing));
