import React, { Component, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import ApplyGiftCard from './apply-gift-card';
import CreditCardBox from '../../credit-cards/card-box';
import CreditCardDetails from '../../credit-cards/card-details';
import { Dropdown, DropdownItem } from '../../dropdown';
import { Form, FormField } from '../../forms';
import NewCreditCard from './new-credit-card';
import SaveCancel from '../../common/save-cancel';
import TileSelector from '../../tile-selector/tile-selector';
import TableCell from '../../table/cell';
import TableRow from '../../table/row';

import * as CreditCardActions from '../../../modules/customers/credit-cards';
import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

function mapStateToProps(state, props) {
  return {
    creditCards: state.customers.creditCards[props.customerId],
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
    creditCards: PropTypes.object,
    customerId: PropTypes.number.isRequired,
    fetchCreditCards: PropTypes.func.isRequired,
    orderPaymentMethodNewCreditCard: PropTypes.func.isRequired,
    paymentMethods: PropTypes.object,
  };

  static defaultProps = {
    creditCards: {},
  };

  constructor(...args) {
    super(...args);

    this.state = {
      isCreditCardFormVisible: false,
      newCreditCard: {
        isDefault: false,
        address: {
          id: null
        },
      },
      paymentType: null,
      selectedPayment: null,
    };
  }

  get giftCardForm() {
    return <ApplyGiftCard />;
  }

  get creditCardSelector() {
    if (this.state.paymentType == 'giftCard') {
      return this.giftCardForm;
    }
  }

  get formControls() {
    const saveDisabled = _.isNull(this.state.selectedPayment) && !this.hasNewCreditCard;
    const onSave = () => this.props.addOrderCreditCardPayment(
      'BR10004',
      _.get(this.state, 'selectedPayment.id')
    );

    return (
      <SaveCancel
        className="fc-new-order-payment__form-controls"
        saveText="Add Payment Method"
        saveDisabled={saveDisabled}
        onSave={onSave}
        onCancel={this.props.orderPaymentMethodStopAdd} />
    );
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
              <DropdownItem value="storeCredit">Store Card</DropdownItem>
              <DropdownItem value="giftCard">Gift Card</DropdownItem>
            </Dropdown>
          </FormField>
        </Form>
      </div>
    );
  }

  @autobind
  changePaymentType(value) {
    this.setState({ paymentType: value });
  }

  get newCreditCard() {
    if (Object.is(this.state.paymentType, 'creditCard')) {
      return <NewCreditCard customerId={this.props.customerId} />;
    }
  }

  render() {
    return (
      <TableRow>
        <TableCell className="fc-new-order-payment" colspan={3}>
          {this.paymentForm}
          {this.newCreditCard}
          {this.formControls}
        </TableCell>
      </TableRow>
    );
  }
};

export default NewPayment;
