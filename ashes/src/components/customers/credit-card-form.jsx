import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';

import CreditCardForm from '../credit-cards/card-form';

const CustomerCreditCardForm = props => {
  const containerClass = classnames(
    'fc-card-container',
    'fc-credit-cards',
    {'fc-credit-cards-new' : props.isNew },
    {'fc-credit-cards-edit' : !props.isNew }
  );

  return (
    <li className={containerClass}>
      <CreditCardForm
        showSelectedAddress={true}
        className="fc-customer-credit-card-form"
        {...props}
      />
    </li>
  );
};

CustomerCreditCardForm.propTypes = {
  isNew: PropTypes.bool,
};

CustomerCreditCardForm.defaultProps = {
  isNew: false,
};

export default CustomerCreditCardForm;
