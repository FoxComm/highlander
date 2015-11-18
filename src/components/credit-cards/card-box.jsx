import React, { PropTypes } from 'react';
import CreditCardDetails from './card-details';
import EditableItemCardContainer from '../item-card-container/editable-item-card-container';
import { autobind } from 'core-decorators';

export default class CreditCardBox extends React.Component {

  static propTypes = {
    card: PropTypes.object,
    customerId: PropTypes.number.isRequired,
    onDeleteClick: PropTypes.func
  };

  constructor(props, context) {
    super(props, context);
  }

  handleIsDefaultChange() {
    console.log('Is default state changed');
  }

  render() {
    const card = this.props.card;

    return (
      <EditableItemCardContainer className="fc-credit-cards"
                                 checkboxLabel="Default card"
                                 initiallyIsDefault={ card.isDefault }
                                 checkboxClickHandler={ this.props.onDefaultToggle }
                                 deleteHandler={ this.props.onDeleteClick }
                                 editHandler={ this.props.onEditClick } >
        <CreditCardDetails customerId={ this.props.customerId} card={ this.props.card } />
      </EditableItemCardContainer>
    );
  }
}
