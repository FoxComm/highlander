import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import _ from 'lodash';

import CreditCardBox from '../../credit-cards/card-box';
import { Dropdown, DropdownItem } from '../../dropdown';
import { Form, FormField } from '../../forms';
import SaveCancel from '../../common/save-cancel';
import TileSelector from '../../tile-selector/tile-selector';
import TableCell from '../../table/cell';
import TableRow from '../../table/row';

import * as CreditCardActions from '../../../modules/customers/credit-cards';

function mapStateToProps(state, props) {
  return { creditCards: state.customers.creditCards[props.customerId] };
}

function mapDispatchToProps(dispatch, props) {
  return _.transform(CreditCardActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.customerId, ...args));
    };
  });
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
      paymentType: null,
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
          onChooseClick={() => console.log('choose')} />
      );
    });
  }

  get creditCardSelector() {
    if (this.state.paymentType == 'creditCard') {
      return <TileSelector items={this.creditCards} />;
    }
  }

  get formControls() {
    return (
      <SaveCancel
        className="fc-new-order-payment__form-controls fc-col-md-1-1"
        saveText="Add Payment Method"
        saveDisabled={true} />
    );
  }

  get paymentForm() {
    return (
      <div className="fc-new-order-payment__form fc-col-md-1-1">
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

  render() {
    return (
      <TableRow>
        <TableCell colspan={3}>
          {this.paymentForm}
          {this.creditCardSelector}
          {this.formControls}
        </TableCell>
      </TableRow>
    );
  }
};

export default NewPayment;
