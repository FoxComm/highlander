import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import AutoScroll from 'components/common/auto-scroll';
import { Dropdown } from 'components/dropdown';
import ErrorAlerts from 'components/alerts/error-alerts';
import { Form, FormField } from 'components/forms';
import NewGiftCard from './new-gift-card';
import NewStoreCredit from './new-store-credit';
import NewCreditCard from './new-credit-card';
import TableCell from 'components/table/cell';
import TableRow from 'components/table/row';

import * as CreditCardActions from 'modules/customers/credit-cards';
import * as PaymentMethodActions from 'modules/carts/payment-methods';

const SELECT_PAYMENT_FORM = [
  ['creditCard', 'Credit Card'],
  ['storeCredit', 'Store Credit'],
  ['giftCard', 'Gift Card'],
];

function mapStateToProps(state, props) {
  return {
    paymentMethods: state.carts.paymentMethods,
  };
}

function mapDispatchToProps(dispatch, props) {
  const payActions = bindActionCreators(PaymentMethodActions, dispatch);
  const ccActions = _.transform(CreditCardActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.customerId, ...args));
    };
  });

  return { ...ccActions, ...payActions };
}

@connect(mapStateToProps, mapDispatchToProps)
class NewPayment extends Component {
  static propTypes = {
    customerId: PropTypes.number.isRequired,
    fetchCreditCards: PropTypes.func.isRequired,
    order: PropTypes.object.isRequired,
    paymentMethods: PropTypes.object,
    cancelAction: PropTypes.func.isRequired,
  };

  state = {
    paymentType: null,
  };

  get errorMessages() {
    return <ErrorAlerts error={this.props.paymentMethods.err} />;
  }

  get newPaymentMethod() {
    const { customerId, order, cancelAction } = this.props;

    const newPaymentProps = {
      order,
      customerId,
      cancelAction,
    };

    switch (this.state.paymentType) {
      case 'creditCard':
        return <NewCreditCard {...newPaymentProps} />;
      case 'giftCard':
        return <NewGiftCard {...newPaymentProps} />;
      case 'storeCredit':
        return <NewStoreCredit {...newPaymentProps} />;
    }
  }

  get paymentForm() {
    return (
      <div className="fc-new-order-payment__form">
        {this.errorMessages}
        <Form>
          <h2 className="fc-new-order-payment__form-title">
            New Payment Method
          </h2>
          <FormField
            className="fc-new-order-payment__payment-type"
            labelClassName="fc-new-order-payment__payment-type-label"
            label="Payment Type">
            <Dropdown
              id="payment-type-dd"
              name="paymentType"
              value={this.state.paymentType}
              onChange={this.changePaymentType}
              items={SELECT_PAYMENT_FORM}
            />
          </FormField>
        </Form>
        <AutoScroll />
      </div>
    );
  }

  @autobind
  changePaymentType(value) {
    this.setState({ paymentType: value });
  }


  render() {
    return (
      <TableRow>
        <TableCell className="fc-new-order-payment" colSpan={5}>
          {this.paymentForm}
          {this.newPaymentMethod}
        </TableCell>
      </TableRow>
    );
  }
}

export default NewPayment;
