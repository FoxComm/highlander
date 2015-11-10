import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import CreditCardBox from '../credit-cards/card-box';
import EditCreditCardBox from '../credit-cards/edit-card-box';
import NewCreditCardBox from '../credit-cards/new-card-box';
import ConfirmationDialog from '../modal/confirmation-dialog';
import { AddButton } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomerCreditCardActions from '../../modules/customers/credit-cards';

@connect((state, props) => ({
  ...state.customers.creditCards[props.customerId]
}), CustomerCreditCardActions)
export default class CustomerCreditCards extends React.Component {

  ////
  // Component default methods
  static propTypes = {
    customerId: PropTypes.number.isRequired,
    fetchCreditCards: PropTypes.func,
    cards: PropTypes.array
  }

  componentDidMount() {
    const customer = this.props.customerId;
    this.props.fetchCreditCards(customer);
  }

  ////
  // Handlers for adding new credit card
  @autobind
  onAddClick() {
    const customer = this.props.customerId;
    this.props.newCustomerCreditCard(customer);
  }

  @autobind
  onAddingCancel() {
    const customer = this.props.customerId;
    this.props.closeNewCustomerCreditCard(customer);
  }

  @autobind
  onChangeNewFormValue({target}) {
    const customer = this.props.customerId;
    this.props.changeNewCustomerCreditCardFormData(customer, target.name, target.value || target.checked);
  }

  @autobind
  onSubmitNewForm(event) {
    event.preventDefault();
    const customer = this.props.customerId;
    this.props.createCreditCard(customer);
  }

  ////
  // Handlers for deleting credit card
  @autobind
  onDeleteClick(cardId) {
    const customer = this.props.customerId;
    this.props.deleteCustomerCreditCard(customer, cardId);
  }

  @autobind
  onDeleteCancel() {
    const customer = this.props.customerId;
    this.props.closeDeleteCustomerCreditCard(customer);
  }

  @autobind
  onDeleteConfirm() {
    const customer = this.props.customerId;
    this.props.confirmCreditCardDeletion(customer);
  }

  ////
  // Handlers for editing credit card
  @autobind
  onEditClick(cardId) {
    const customer = this.props.customerId;
    this.props.editCustomerCreditCard(customer, cardId);
  }

  @autobind
  onEditCancel() {
    const customer = this.props.customerId;
    this.props.closeEditCustomerCreditCard(customer);
  }

  @autobind
  onEditFormChange({target}) {
    const customer = this.props.customerId;
    this.props.changeEditCustomerCreditCardFormData(customer, target.name, target.value || target.checked);
  }

  @autobind
  onEditFormSubmit(event) {
    event.preventDefault();
    const customer = this.props.customerId;
    this.props.saveCreditCard(customer);
  }

  ////
  // Rendering
  render() {
    const actionBlock = (
      <AddButton onClick={this.onAddClick} />
    );

    const createCardBox = (card) => {
      const key = `cutomer-card-${ card.id }`;
      let box = null;
      if (card.id === this.props.editingId) {
        box = (
          <EditCreditCardBox key={ key }
                             card={ card }
                             form={ this.props.editingCreditCard }
                             customerId={ this.props.customerId }
                             onCancel={ this.onEditCancel }
                             onChange={ this.onEditFormChange }
                             onSubmit={ this.onEditFormSubmit } />
        );
      } else {
        box = (
          <CreditCardBox key={ key }
                         card={ card }
                         customerId={ this.props.customerId }
                         onDeleteClick={ this.onDeleteClick.bind(this, card.id) }
                         onEditClick={ this.onEditClick.bind(this, card.id) } />
        );
      }
      return box;
    };

    const showConfirm = this.props.deletingId !== undefined && this.props.deletingId !== null;

    return (
      <ContentBox title="Credit Cards"
                  className="fc-customer-credit-cards"
                  actionBlock={ actionBlock }>
        <ul className="fc-float-list">
          {(this.props.cards && this.props.cards.map(createCardBox))}
          {(this.props.newCreditCard && <NewCreditCardBox customerId={ this.props.customerId }
                                                          form={ this.props.newCreditCard }
                                                          onCancel={ this.onAddingCancel }
                                                          onSubmit={ this.onSubmitNewForm }
                                                          onChange={ this.onChangeNewFormValue }/>)}
        </ul>
        <ConfirmationDialog
          isVisible={ showConfirm }
          header='Confirm'
          body='Are you sure you want to delete this credit card?'
          cancel='Cancel'
          confirm='Yes, Delete'
          cancelAction={ this.onDeleteCancel }
          confirmAction={ this.onDeleteConfirm } />
      </ContentBox>
    );
  }
}
