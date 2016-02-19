import React, { Component, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import AutoScroll from '../../common/auto-scroll';
import CreditCardBox from '../../credit-cards/card-box';
import CreditCardDetails from '../../credit-cards/card-details';
import { Dropdown, DropdownItem } from '../../dropdown';
import { Form, FormField } from '../../forms';
import NewGiftCard from './new-gift-card';
import NewStoreCredit from './new-store-credit';
import NewCreditCard from './new-credit-card';
import TileSelector from '../../tile-selector/tile-selector';
import TableCell from '../../table/cell';
import TableRow from '../../table/row';

import * as CreditCardActions from '../../../modules/customers/credit-cards';
import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

function mapStateToProps(state, props) {
  return {
    paymentMethods: state.orders.paymentMethods,
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
    orderPaymentMethodNewCreditCard: PropTypes.func.isRequired,
    paymentMethods: PropTypes.object,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      paymentType: null,
    };
  }

  get newPaymentMethod() {
    const { customerId, order } = this.props;

    switch(this.state.paymentType) {
    case 'creditCard':
      return <NewCreditCard order={order} customerId={customerId} />;
    case 'giftCard':
      return <NewGiftCard order={order} customerId={customerId} />;
    case 'storeCredit':
      return <NewStoreCredit order={order} customerId={customerId} />;
    }
  }

  get paymentForm() {
    return (
      <div className="fc-new-order-payment__form">
        <Form>
          <h2 className="fc-new-order-payment__form-title">
            New Payment Method
          </h2>
          <FormField
            className="fc-new-order-payment__payment-type"
            labelClassName="fc-new-order-payment__payment-type-label"
            label="Payment Type">
            <Dropdown
              name="paymentType"
              value={this.state.paymentType}
              onChange={this.changePaymentType}>
              <DropdownItem value="creditCard">Credit Card</DropdownItem>
              <DropdownItem value="storeCredit">Store Credit</DropdownItem>
              <DropdownItem value="giftCard">Gift Card</DropdownItem>
            </Dropdown>
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
        <TableCell className="fc-new-order-payment" colspan={3}>
          {this.paymentForm}
          {this.newPaymentMethod}
        </TableCell>
      </TableRow>
    );
  }
};

export default NewPayment;
