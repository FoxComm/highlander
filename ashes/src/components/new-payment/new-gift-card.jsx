import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as CartActions from 'modules/carts/details';
import * as PaymentMethodActions from 'modules/carts/payment-methods';

import DebitCredit from 'components/payment-row/debit-credit';
import { Form, FormField } from 'components/forms';
import Alert from '../alerts/alert';

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

    if (!isSearchingGiftCards &&
      gcResults.length == 1 &&
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
      return <Alert type="warning">{`Gift Card ${giftCardCode} not found`}</Alert>;
    }

    return null;
  }

  @autobind
  handleGiftCardChange({target}) {
    this.setState({
      giftCardCode: target.value,
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
            <input type="text"
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
