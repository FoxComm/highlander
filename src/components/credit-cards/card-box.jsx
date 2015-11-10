import React, { PropTypes } from 'react';
import CreditCardDetails from './card-details';
import EditableItemCardContainer from '../item-card-container/editable-item-card-container';

export default class CreditCardBox extends React.Component {

  static propTypes = {
    card: PropTypes.object,
    customerId: PropTypes.number.isRequired,
  };

  constructor(props, context) {
    super(props, context);
  }

  handleIsDefaultChange() {
    console.log('Is default state changed');
  }

  handleDeleteClick() {
    console.log('Delete button action triggered');
  }

  render() {
    let card = this.props.card;

    return (
      <EditableItemCardContainer className="fc-credit-cards"
                                 checkboxLabel="Default card"
                                 initiallyIsDefault={ card.isDefault }
                                 checkboxClickHandler={ this.handleIsDefaultChange }
                                 deleteHandler={ this.handleDeleteClick }>
        <CreditCardDetails customerId={ this.props.customerId} card={ this.props.card } />
      </EditableItemCardContainer>
    );
  }
}
