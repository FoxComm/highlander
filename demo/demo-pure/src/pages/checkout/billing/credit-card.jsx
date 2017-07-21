/* @flow weak */

// libs
import React from 'react';
import classNames from 'classnames';

// components
import Radiobutton from 'ui/radiobutton/radiobutton';
import ViewBilling from './view-billing';
import ActionLink from 'ui/action-link/action-link';

// types
import type { CreditCardType } from '../types';

// styles
import styles from './credit-card.css';


type Props = {
  creditCard: CreditCardType,
  selected: boolean,
  onSelect: (cc: CreditCardType) => void,
  onEditCard: (creditCard: CreditCardType) => void,
  onDeleteCard: (id: number) => void,
  allowEdit?: boolean,
};

const CreditCard = (props: Props) => {
  const { creditCard, selected, onSelect, onDeleteCard } = props;
  const { id } = creditCard;
  const cardClasses = classNames(styles['credit-card'], {
    [styles.chosen]: selected,
  });

  return (
    <div key={id} className={cardClasses}>
      <Radiobutton
        name="credit-card"
        checked={selected}
        onChange={() => onSelect(creditCard)}
        id={`credit-card-${id}`}
      >
        <ViewBilling billingData={creditCard} inModal />
      </Radiobutton>
      <ActionLink
        action={() => onDeleteCard(id)}
        title="Remove"
        styleName="action-link-remove-card"
      />
    </div>
  );
};

export default CreditCard;
