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

// styles
import styles from './billing.css';

// types
import type { CheckoutBlockProps, BillingData } from '../types';

type Props = CheckoutBlockProps & {
  paymentMethods: Array<any>,
  creditCard: BillingData,
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
      if (props.paymentMethods.length > 0) {
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
          action={props.toggleShippingModal}
          title={title}
          styleName="action-link-payment"
          icon={icon}
        />
      );
    }
  }

  get content() {
    const { coupon, promotion, totals, creditCard } = this.props;

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
  };
};

export default connect(mapStateToProps)(localized(Billing));
