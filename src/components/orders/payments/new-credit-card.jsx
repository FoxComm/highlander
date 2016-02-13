  import React, { Component, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import CreditCardBox from '../../credit-cards/card-box';
import CreditCardDetails from '../../credit-cards/card-details';
import CreditCardForm from '../../credit-cards/card-form';
import TileSelector from '../../tile-selector/tile-selector';

import * as AddressActions from '../../../modules/customers/addresses';
import * as CreditCardActions from '../../../modules/customers/credit-cards';
import * as PaymentMethodActions from '../../../modules/orders/payment-methods';

function mapStateToProps(state, props) {
  return {
    addresses: state.customers.addresses[props.customerId],
    creditCards: state.customers.creditCards[props.customerId],
    paymentMethods: state.orders.paymentMethods,
  };
}

function mapActionsToCustomer(dispatch, actions, customerId) {
  return _.transform(actions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(customerId, ...args));
    };
  });
}

function mapDispatchToProps(dispatch, props) {
  return {
    ...mapActionsToCustomer(dispatch, AddressActions, props.customerId),
    ...mapActionsToCustomer(dispatch, CreditCardActions, props.customerId),
    ...bindActionCreators(dispatch, PaymentMethodActions, dispatch),
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class NewCreditCard extends Component {
  static propTypes = {
    addresses: PropTypes.object.isRequired,
    creditCards: PropTypes.object.isRequired,
    customerId: PropTypes.number.isRequired,
    fetchAddresses: PropTypes.func.isRequired,
    fetchCreditCards: PropTypes.func.isRequired,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      newCard: null,
      selectedCard: null,
      showForm: false,
    };
  }

  componentDidMount() {
    this.props.fetchCreditCards();
    this.props.fetchAddresses();
  }

  get creditCards() {
    const creditCards = _.get(this.props, 'creditCards.cards', []);
    return creditCards.map(card => {
      return (
        <CreditCardBox card={card}
                       customerId={this.props.customerId}
                       onChooseClick={() => this.setState({ newCard: card })} />
      );
    });
  }

  get creditCardForm() {
    if (this.state.showForm) {
      return (
        <div className="fc-order-credit-card-form">
          <CreditCardForm card={this.state.newCard}
                          customerId={this.props.customerId}
                          addresses={this.props.addresses.addresses}
                          form={this.state.newCard}
                          isDefaultEnabled={false}
                          showFormControls={false}
                          isNew={true}
                          onChange={this.handleCreditCardChange}
                          showSelectedAddress={true} />
        </div>
      );
    }
  }

  get creditCardTiles() {
    if (!this.state.showForm) {
      return (
        <TileSelector items={this.creditCards}
                      onAddClick={this.toggleCreditCardForm}
                      title="Customer's Credit Cards" />
      );
    }
  }

  get selectedCard() {
    if (!_.isNull(this.state.selectedCard)) {
      return (
        <CreditCardDetails customerId={this.props.customerId} card={this.state.selectedCard} />
      );
    }
  }

  @autobind
  handleCreditCardChange({target}) {
    const address = Object.is(target.name, 'addressId')
      ? _.find(this.props.addresses.addresses, { id: target.value })
      : this.state.newCard.address;

    this.setState(assoc(this.state,
      ['newCard', target.name], target.value,
      ['newCard', 'address'], address
    ));
  }

  @autobind
  toggleCreditCardForm() {
    this.setState({
      newCard: {
        isDefault: false,
        holderName: null,
        number: null,
        cvv: null,
        expMonth: null,
        expYear: null,
        address: {
          id: null,
        },
      },
      showForm: !this.state.showForm,
    });
  }

  render() {
    return (
      <div>
        {this.selectedCard}
        {this.creditCardForm}
        {this.creditCardTiles}
      </div>
    );
  }
}
