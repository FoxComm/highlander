import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import CreditCardBox from 'components/credit-cards/card-box';
import CreditCardDetails from 'components/credit-cards/card-details';
import CreditCardForm from 'components/credit-cards/card-form';
import SaveCancel from 'components/common/save-cancel';
import TileSelector from 'components/tile-selector/tile-selector';

import * as CreditCardActions from 'modules/customers/credit-cards';
import * as CartDetailsActions from 'modules/carts/details';
import * as PaymentMethodActions from 'modules/carts/payment-methods';

function mapStateToProps(state, props) {
  return {
    creditCards: state.customers.creditCards[props.customerId],
    paymentMethods: state.carts.paymentMethods,
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
      ...mapActionsToCustomer(dispatch, CreditCardActions, props.customerId),
      ...bindActionCreators(CartDetailsActions, dispatch),
      ...bindActionCreators(PaymentMethodActions, dispatch),
    },
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class NewCreditCard extends Component {
  static propTypes = {
    creditCards: PropTypes.object,
    customerId: PropTypes.number.isRequired,
    order: PropTypes.object.isRequired,
    cancelAction: PropTypes.func.isRequired,

    actions: PropTypes.shape({
      selectCreditCard: PropTypes.func,
      createAndAddOrderCreditCardPayment: PropTypes.func,
      fetchAddresses: PropTypes.func,
      fetchCreditCards: PropTypes.func,
    }).isRequired,
  };

  state = {
    newCard: null,
    selectedCard: null,
    showForm: false,
  };

  componentDidMount() {
    this.props.actions.fetchCreditCards();
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
          <CreditCardForm
            card={this.state.newCard}
            customerId={this.props.customerId}
            isDefaultEnabled={false}
            isNew={true}
            onCancel={() => this.setState({ showForm: false })}
            onSubmit={this.handleCreditCardSubmit}
            saveText="Add Payment Method"
          />
        </div>
      );
    }
  }

  get creditCardTiles() {
    if (!this.state.showForm && _.isNull(this.state.selectedCard)) {
      const isFetching = _.get(this.props, 'creditCards.isFetching', null);
      return (
        <TileSelector
          addButtonId="payment-methods-create-credit-cart"
          isFetching={isFetching}
          items={this.creditCards}
          onAddClick={this.toggleCreditCardForm}
          title="Customer's Credit Cards"
        />
      );
    }
  }

  get formControls() {
    if (!this.state.showForm) {
      const saveDisabled = _.isNull(this.state.selectedCard);
      const onSave = () => this.props.actions.selectCreditCard(
        this.props.order.referenceNumber,
        _.get(this.state, 'selectedCard.id')
      );

      return (
        <SaveCancel className="fc-new-order-payment__form-controls"
                    saveText="Add Payment Method"
                    saveDisabled={saveDisabled}
                    onSave={onSave}
                    onCancel={this.props.cancelAction} />
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
