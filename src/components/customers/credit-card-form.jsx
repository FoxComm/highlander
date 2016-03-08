import React, { PropTypes } from 'react';
import classnames from 'classnames';
import _ from 'lodash';

import CreditCardForm from '../credit-cards/card-form';

const CustomerCreditCardForm = props => {
  const { isNew, ...rest } = props;
  const containerClass = classnames(
    'fc-card-container',
    'fc-credit-cards',
    {'fc-credit-cards-new' : isNew },
    {'fc-credit-cards-edit' : !isNew }
  );

  return (
    <li className={containerClass}>
      <CreditCardForm className="fc-customer-credit-card-form"
                      isNew={isNew}
                      {...rest} />
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
