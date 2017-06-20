import React from 'react';
import PropTypes from 'prop-types';
import CreditCardDetails from './card-details';
import EditableItemCardContainer from '../item-card-container/editable-item-card-container';

const CreditCardBox = (props) => {
  const card = props.card;

  return (
    <EditableItemCardContainer className="fc-credit-cards"
                               id={`credit-card-${card.id}`}
                               checkboxLabel="Default Card"
                               isDefault={ card.isDefault }
                               checkboxChangeHandler={ props.onDefaultToggle }
                               deleteHandler={ props.onDeleteClick }
                               editHandler={ props.onEditClick }
                               chooseHandler={ props.onChooseClick }>
      <CreditCardDetails customerId={ props.customerId} card={ card } />
    </EditableItemCardContainer>
  );
};

CreditCardBox.propTypes = {
  card: PropTypes.object,
  customerId: PropTypes.number.isRequired,
  onDeleteClick: PropTypes.func,
  onDefaultToggle: PropTypes.func,
  onEditClick: PropTypes.func,
  onChooseClick: PropTypes.func,
};

export default CreditCardBox;
