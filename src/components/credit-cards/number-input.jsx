import React, { PropTypes } from 'react';

import { FormField } from '../forms';

const CreditCardNumberInput = props => {
  return (
    <FormField className={props.className} label="Credit Card Number">
      <div className="fc-credit-card-number-input__input-container">
        <input type="text" name="number" />
        <div className="fc-credit-card-number-input__cc-type _visa"></div>
      </div>
    </FormField>
  );
};

CreditCardNumberInput.propTypes = {
  className: PropTypes.string,
};

export default CreditCardNumberInput;

