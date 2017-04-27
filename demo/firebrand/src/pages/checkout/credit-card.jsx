import React from 'react';

import Icon from 'ui/icon';
import Checkbox from 'ui/checkbox';

import styles from './checkout.css';

type CreditCardType = {
  id: number;
  brand: string;
  lastFour: string;
  expMonth: number;
  expYear: number;
};

type Props = {
  creditCard: CreditCardType;
  selected: boolean;
  onSelect: (cc: CreditCardType) => void;
}

const CreditCard = (props: Props) => {
  const { creditCard, selected, onSelect } = props;
  const { id, brand, lastFour, expMonth, expYear } = creditCard;

  return (
    <div key={id} styleName="credit-card">
      <Checkbox
        name="credit-card"
        checked={selected}
        onChange={() => onSelect(creditCard)}
        id={`credit-card-${id}`}
      >
        <span>
          <span>•••• {lastFour}</span>
          <span styleName="credit-card-valid">
            {expMonth}/{expYear.toString().slice(-2)}
          </span>
        </span>
      </Checkbox>
      <Icon styleName="payment-icon" name={`fc-payment-${brand.toLowerCase()}`} />
    </div>
  );
};

export default CreditCard;
