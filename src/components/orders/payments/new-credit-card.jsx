import React, { Component, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import CreditCardBox from '../../credit-cards/card-box';
import CreditCardDetails from '../../credit-cards/card-details';
import CreditCardForm from '../../credit-cards/card-form';
import SaveCancel from '../../common/save-cancel';
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
    actions: {
      ...mapActionsToCustomer(dispatch, AddressActions, props.customerId),
      ...mapActionsToCustomer(dispatch, CreditCardActions, props.customerId),
      ...bindActionCreators(PaymentMethodActions, dispatch),
    },
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class NewCreditCard extends Component {
  static propTypes = {
    addresses: PropTypes.object.isRequired,
    creditCards: PropTypes.object.isRequired,
    customerId: PropTypes.number.isRequired,
    order: PropTypes.object.isRequired,

    actions: PropTypes.shape({
      addOrderCreditCardPayment: PropTypes.func.isRequired,
      createAndAddOrderCreditCardPayment: PropTypes.func.isRequired,
      fetchAddresses: PropTypes.func.isRequired,
      fetchCreditCards: PropTypes.func.isRequired,
      orderPaymentMethodStopAdd: PropTypes.func.isRequired,
    }).isRequired,
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
    this.props.actions.fetchCreditCards();
    this.props.actions.fetchAddresses();
  }

  get creditCards() {
    const creditCards = _.get(this.props, 'creditCards.cards', []);
    return creditCards.map(card => {
      return (
        <CreditCardBox card={card}
                       customerId={this.props.customerId}
                       onChooseClick={() => this.selectCard(card)} />
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
                          onCancel={() => this.setState({ showForm: false })}
                          onSubmit={this.handleCreditCardSubmit}
                          saveText="Add Payment Method"
                          showSelectedAddress={true} />
        </div>
      );
    }
  }

  get creditCardTiles() {
    if (!this.state.showForm && _.isNull(this.state.selectedCard)) {
      return (
        <TileSelector items={this.creditCards}
                      onAddClick={this.toggleCreditCardForm}
                      title="Customer's Credit Cards" />
      );
    }
  }

  get formControls() {
    if (!this.state.showForm) {
      const saveDisabled = _.isNull(this.state.selectedCard);
      const onSave = () => this.props.actions.addOrderCreditCardPayment(
        this.props.order.referenceNumber,
        _.get(this.state, 'selectedCard.id')
      );

      return (
        <SaveCancel className="fc-new-order-payment__form-controls"
                    saveText="Add Payment Method"
                    saveDisabled={saveDisabled}
                    onSave={onSave}
                    onCancel={this.props.actions.orderPaymentMethodStopAdd} />
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
  handleCreditCardSubmit(event, creditCard) {
    this.props.actions.createAndAddOrderCreditCardPayment(
      this.props.order.referenceNumber,
      creditCard,
      this.props.customerId
    );
  }

  @autobind
  selectCard(card) {
    this.setState({
      selectedCard: card,
    });
  }

  @autobind
  toggleCreditCardForm() {
    this.setState({
      newCard: null,
      showForm: !this.state.showForm,
    });
  }

  render() {
    return (
      <div>
        {this.selectedCard}
        {this.creditCardForm}
        {this.creditCardTiles}
        {this.formControls}
      </div>
    );
  }
}
