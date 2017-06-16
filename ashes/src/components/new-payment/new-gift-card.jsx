// libs
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as CartActions from 'modules/carts/details';
import * as PaymentMethodActions from 'modules/carts/payment-methods';

// components
import DebitCredit from 'components/payment-row/debit-credit';
import { Form, FormField } from 'components/forms';
import Alert from 'components/core/alert';
import TextInput from 'components/core/text-input';

function mapStateToProps(state) {
  return {
    paymentMethods: state.carts.paymentMethods,
    isSearchingGiftCards: _.get(state, 'asyncActions.orders/giftCards.inProgress', false),
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: {
      ...bindActionCreators(PaymentMethodActions, dispatch),
      ...bindActionCreators(CartActions, dispatch),
    },
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class NewGiftCard extends Component {
  static propTypes = {
    order: PropTypes.object.isRequired,
    paymentMethods: PropTypes.shape({
      giftCards: PropTypes.array,
    }).isRequired,
    isSearchingGiftCards: PropTypes.bool.isRequired,
    cancelAction: PropTypes.func.isRequired,

    actions: PropTypes.shape({
      addGiftCardPayment: PropTypes.func.isRequired,
      giftCardSearch: PropTypes.func.isRequired,
    }).isRequired,
  };

  state = {
    giftCard: null,
    giftCardCode: '',
    showGiftCardSummary: false,
  };

  componentWillReceiveProps(nextProps) {
    const { isSearchingGiftCards } = nextProps;
    const gcResults = _.get(nextProps, 'paymentMethods.giftCards', []);
    const gcCode = _.get(gcResults, [0, 'code'], '');
    const gcState = _.get(gcResults, [0, 'state'], '');

    if (!isSearchingGiftCards &&
      gcResults.length == 1 &&
      gcState !== 'onHold' &&
      _.startsWith(gcCode.toLowerCase(), this.codeValue.toLowerCase())) {

      this.setState({
        giftCard: gcResults[0],
        giftCardCode: gcCode,
        showGiftCardSummary: true,
      });
    } else {
      this.setState({
        giftCard: null,
        showGiftCardSummary: false,
      });
    }
  }

  get availableBalance() {
    return _.get(this.state, 'giftCard.availableBalance', 0);
  }

  get giftCardSummary() {
    if (this.state.showGiftCardSummary) {
      return (
        <DebitCredit
          availableBalance={this.availableBalance}
          onCancel={this.props.cancelAction}
          onSubmit={this.handleGiftCardSubmit}
        />
      );
    }
  }

  get codeValue() {
    const { giftCardCode } = this.state;
    if (giftCardCode) {
      return giftCardCode.replace(/\s+/g, '');
    }
    return giftCardCode;
  }

  get codeIsValid() {
    const { giftCardCode } = this.state;

    // @todo validation for length & maybe mask
    return !!giftCardCode;
  }

  get error() {
    // @todo figure out why array but not just one
    const { paymentMethods: { giftCards = [] }, isSearchingGiftCards } = this.props;
    const giftCard = giftCards[0];
    const { giftCardCode } = this.state;

    if (!isSearchingGiftCards && this.codeIsValid && !giftCard) {
      return <Alert type={Alert.WARNING}>{`Gift Card ${giftCardCode} not found`}</Alert>;
    }

    if (this.codeIsValid && giftCard && giftCard.state === 'onHold') {
      return (
        <Alert type={Alert.WARNING}>
          {`Gift Card ${giftCardCode} is on hold`}
        </Alert>
      );
    }

    return null;
  }

  @autobind
  handleGiftCardChange(value) {
    this.setState({
      giftCardCode: value,
    }, () => this.props.actions.giftCardSearch(this.codeValue));
  }

  @autobind
  handleGiftCardSubmit(amountToUse) {
    this.props.actions.addGiftCardPayment(
      this.props.order.referenceNumber,
      this.codeValue,
      amountToUse
    );
  }

  render() {
    return (
      <div className="fc-order-apply-gift-card">
        <Form className="fc-form-vertical">
          <FormField className="fc-order-apply-gift-card__card-number"
                     label="Gift Card Number">
            <TextInput
                   name="giftCardCode"
                   onChange={this.handleGiftCardChange}
                   value={this.state.giftCardCode} />
          </FormField>
        </Form>
        {this.error}
        {this.giftCardSummary}
      </div>
    );
  }
}
