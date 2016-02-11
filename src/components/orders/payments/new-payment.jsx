import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import ApplyGiftCard from './apply-gift-card';
import CreditCardBox from '../../credit-cards/card-box';
import CreditCardDetails from '../../credit-cards/card-details';
import { Dropdown, DropdownItem } from '../../dropdown';
import { Form, FormField } from '../../forms';
import OrderCreditCardForm from './credit-card-form';
import SaveCancel from '../../common/save-cancel';
import TileSelector from '../../tile-selector/tile-selector';
import TableCell from '../../table/cell';
import TableRow from '../../table/row';

import * as CreditCardActions from '../../../modules/customers/credit-cards';
import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

function mapStateToProps(state, props) {
  return { creditCards: state.customers.creditCards[props.customerId] };
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
  };

  static defaultProps = {
    creditCards: {},
  };

  constructor(...args) {
    super(...args);

    this.state = {
      isCreditCardFormVisible: false,
      paymentType: null,
      selectedPayment: null,
    };
  }

  componentDidMount() {
    this.props.fetchCreditCards();
  }

  get creditCards() {
    return _.map(_.get(this.props, 'creditCards.cards', []), card => {
      return (
        <CreditCardBox
          card={card}
          customerId={this.props.customerId}
          onChooseClick={() => this.selectCreditCard(card)} />
      );
    });
  }

  get creditCardForm() {
    return <OrderCreditCardForm customerId={1} />;
  }

  get giftCardForm() {
    return <ApplyGiftCard />;
  }

  get creditCardSelector() {
    if (this.state.paymentType == 'creditCard' && _.isNull(this.state.selectedPayment)) {
      if (this.state.isCreditCardFormVisible) {
        return this.creditCardForm;
      } else {
        return this.creditCardTiles;
      }
    } else if (this.state.paymentType == 'giftCard') {
      return this.giftCardForm;
    }
  }

  get creditCardTiles() {
    return (
      <TileSelector
        items={this.creditCards}
        onAddClick={this.toggleCreditCardForm}
        title="Customer's Credit Cards" />
    );
  }

  get formControls() {
    const saveDisabled = _.isNull(this.state.selectedPayment);
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

  get selectedPayment() {
    const paymentType = _.get(this.state, 'selectedPayment.type');
    switch (paymentType) {
      case 'creditCard':
        const card = this.state.selectedPayment;
        return <CreditCardDetails customerId={this.props.customerId} card={card} />;
    }
  }

  @autobind
  changePaymentType(value) {
    this.setState({ paymentType: value });
  }

  @autobind
  selectCreditCard(card) {
    this.setState({
      selectedPayment: {
        type: 'creditCard',
        ...card,
      },
    });
  }

  @autobind
  toggleCreditCardForm() {
    this.setState({
      isCreditCardFormVisible: !this.state.isCreditCardFormVisible
    });
  }

  render() {
    return (
      <TableRow>
        <TableCell className="fc-new-order-payment" colspan={3}>
          {this.paymentForm}
          {this.selectedPayment}
          {this.creditCardSelector}
          {this.formControls}
        </TableCell>
      </TableRow>
    );
  }
};

export default NewPayment;
