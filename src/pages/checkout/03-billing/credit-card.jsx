/* @flow weak */

// libs
import React from 'react';

// components
import Radiobutton from 'ui/radiobutton/radiobutton';
import ViewBilling from './view-billing';

// types
import type { CreditCardType } from '../types';

// styles
import styles from './credit-card.css';


type Props = {
  creditCard: CreditCardType,
  selected: boolean,
  onSelect: (cc: CreditCardType) => void,
  editCard: Function,
  deleteCard: Function,
};

const CreditCard = (props: Props) => {
  const { creditCard, selected, onSelect, editCard, deleteCard } = props;
  const { id } = creditCard;

  return (
    <div key={id} styleName="credit-card">
      <Radiobutton
        name="credit-card"
        checked={selected}
        onChange={() => onSelect(creditCard)}
        id={`credit-card-${id}`}
      >
        <ViewBilling billingData={creditCard} />
      </Radiobutton>
      <div styleName="actions">
        <span styleName="action" onClick={() => editCard(creditCard)}>Edit</span>
        <span styleName="action" onClick={() => deleteCard(id)}>Delete</span>
      </div>
    </div>
  );
};

export default CreditCard;
