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

import { togglePaymentModal, fetchCreditCards } from 'modules/checkout';

import type { CheckoutBlockProps, BillingData } from '../types';
import type { AsyncStatus } from 'types/async-actions';

import styles from './billing.css';

type Props = CheckoutBlockProps & {
  paymentMethods: Array<any>,
  creditCard: BillingData,
  paymentModalVisible: boolean,
  loginState: AsyncStatus,
};

type State = {
  fetchedCreditCards: boolean,
};

class Billing extends Component {
  props: Props;

  state: State = {
    fetchedCreditCards: false,
  };

  componentWillMount() {
    this.fetchCreditCards();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.loginState.finished && !this.state.fetchedCreditCards) {
      this.fetchCreditCards();
    } else if (nextProps.loginState.inProgress) {
      this.setState({ fetchedCreditCards: false });
    }
  }

  fetchCreditCards() {
    this.props.fetchCreditCards().then(() => {
      this.setState({ fetchedCreditCards: true });
    });
  }

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

    if (!this.state.fetchedCreditCards) return null;

    const hasCards = !_.isEmpty(props.creditCards);
    const icon = {
      name: 'fc-plus',
      className: styles.plus,
    };
    const title = hasCards ? 'Choose' : 'Add new';
    const addIcon = !hasCards ? icon : null;

    return (
      <ActionLink
        action={props.togglePaymentModal}
        title={title}
        styleName="action-link-payment"
        icon={addIcon}
      />
    );
  }

  get coupon() {
    const { coupon } = this.props;
    if (_.isEmpty(coupon)) return null;

    return (
      <div styleName="promo-line">
        <PromoCode
          coupon={coupon}
          allowDelete={false}
        />
      </div>
    );
  }

  get giftCard() {
    if (_.isEmpty(this.giftCards)) return null;

    return (
      <div styleName="promo-line">
        <PromoCode
          giftCards={this.giftCards}
          allowDelete={false}
        />
      </div>
    );
  }

  get content() {
    if (!this.state.fetchedCreditCards) return <Loader size="m" />;

    const { creditCard, paymentModalVisible } = this.props;

    return (
      <div styleName="billing-summary">
        <ViewBilling billingData={creditCard} />
        {this.giftCard}
        {this.coupon}
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
    creditCards: _.get(state.checkout, 'creditCards', []),
    ...state.cart,
    cartState: _.get(state.asyncActions, 'cart', {}),
    paymentModalVisible: _.get(state.checkout, 'paymentModalVisible', false),
    loginState: _.get(state.asyncActions, 'auth-login', {}),
  };
};

export default connect(mapStateToProps, {
  togglePaymentModal,
  fetchCreditCards,
})(localized(Billing));
