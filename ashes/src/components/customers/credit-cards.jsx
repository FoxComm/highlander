import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

import * as CustomerCreditCardActions from '../../modules/customers/credit-cards';

import ContentBox from '../content-box/content-box';
import EmptyText from '../content-box/empty-text';
import CreditCardBox from '../credit-cards/card-box';
import ConfirmationDialog from '../modal/confirmation-dialog';
import CustomerCreditCardForm from './credit-card-form';
import { AddButton } from '../common/buttons';

function mapDispatchToProps(dispatch, props) {
  return _.transform(CustomerCreditCardActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.customerId, ...args));
    };
  });
}

@connect((state, props) => ({
  ...state.customers.creditCards[props.customerId]
}), mapDispatchToProps)
export default class CustomerCreditCards extends React.Component {

  ////
  // Component default methods
  static propTypes = {
    customerId: PropTypes.number.isRequired,
    fetchCreditCards: PropTypes.func,
    cards: PropTypes.array,
  };

  componentDidMount() {
    this.props.fetchCreditCards();
  }

  ////
  // Handlers for adding new credit card
  @autobind
  onAddClick() {
    this.props.newCustomerCreditCard();
  }

  @autobind
  onAddingCancel() {
    this.props.closeNewCustomerCreditCard();
  }

  @autobind
  onChangeNewFormValue({target}) {
    this.props.changeNewCustomerCreditCardFormData(target.name, target.value || target.checked);
  }

  @autobind
  onSubmitNewForm(event) {
    event.preventDefault();
    this.props.createCreditCard();
  }

  ////
  // Handlers for deleting credit card
  @autobind
  onDeleteClick(cardId) {
    this.props.deleteCustomerCreditCard(cardId);
  }

  @autobind
  onDeleteCancel() {
    this.props.closeDeleteCustomerCreditCard();
  }

  @autobind
  onDeleteConfirm() {
    this.props.confirmCreditCardDeletion();
  }

  ////
  // Handlers for editing credit card
  @autobind
  onEditClick(cardId) {
    this.props.editCustomerCreditCard(cardId);
  }

  @autobind
  onEditCancel() {
    this.props.closeEditCustomerCreditCard();
  }

  @autobind
  onEditFormChange({target}) {
    this.props.changeEditCustomerCreditCardFormData(target.name, target.value || target.checked);
  }

  @autobind
  onEditFormSubmit(event) {
    event.preventDefault();
    this.props.saveCreditCard();
  }

  get showConfirm() {
    return this.props.deletingId !== undefined && this.props.deletingId !== null;
  }

  get actionBlock() {
    return <AddButton onClick={this.onAddClick} />;
  }

  @autobind
  createCardBox(card) {
    const key = `cutomer-card-${ card.id }`;
    let box = null;
    if (card.id === this.props.editingId) {
      box = (
        <CustomerCreditCardForm
          key={ key }
          card={ card }
          form={ this.props.editingCreditCard }
          customerId={ this.props.customerId }
          onCancel={ this.onEditCancel }
          onChange={ this.onEditFormChange }
          onSubmit={ this.onEditFormSubmit }
          isNew={ false } />
      );
    } else {
      box = (
        <CreditCardBox
          key={ key }
          card={ card }
          customerId={ this.props.customerId }
          onDeleteClick={ () => this.onDeleteClick(card.id) }
          onEditClick={ () => this.onEditClick(card.id) }
          onDefaultToggle={ () => this.props.toggleDefault(card.id) } />
      );
    }
    return box;
  }

  get emptyText() {
    return <EmptyText label="No saved credit cards." />;
  }

  ////
  // Rendering
  render() {
    const {props} = this;
    const {cards} = props;

    return (
      <ContentBox title="Credit Cards"
                  className="fc-customer-credit-cards"
                  actionBlock={ this.actionBlock }>
        <ul className="fc-float-list">
          {props.newCreditCard || (cards && cards.length)
            ? cards.map(this.createCardBox)
            : this.emptyText}
          {props.newCreditCard ?
            <CustomerCreditCardForm
              customerId={ props.customerId }
              card={ props.newCreditCard }
              onCancel={ this.onAddingCancel }
              onSubmit={ this.onSubmitNewForm }
              onChange={ this.onChangeNewFormValue }
              isNew={ true } /> : null}
        </ul>
        <ConfirmationDialog
          isVisible={ this.showConfirm }
          header='Confirm'
          body='Are you sure you want to delete this credit card?'
          cancel='Cancel'
          confirm='Yes, Delete'
          onCancel={ this.onDeleteCancel }
          confirmAction={ this.onDeleteConfirm } />
      </ContentBox>
    );
  }
}
