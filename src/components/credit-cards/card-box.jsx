import React, { PropTypes } from 'react';
import CreditCardDetails from './card-details';
import EditableItemCardContainer from '../item-card-container/editable-item-card-container';
import { autobind } from 'core-decorators';

const CreditCardBox = (props) => {
  const card = props.card;

  return (
    <EditableItemCardContainer className="fc-credit-cards"
                               checkboxLabel="Default card"
                               isDefault={ card.isDefault }
                               checkboxChangeHandler={ props.onDefaultToggle }
                               deleteHandler={ props.onDeleteClick }
                               editHandler={ props.onEditClick } >
      <CreditCardDetails customerId={ props.customerId} card={ card } />
    </EditableItemCardContainer>
  );
};

CreditCardBox.propTypes = {
  card: PropTypes.object,
  customerId: PropTypes.number.isRequired,
  onDeleteClick: PropTypes.func,
  onDefaultToggle: PropTypes.func,
  onEditClick: PropTypes.func
};

export default CreditCardBox;
