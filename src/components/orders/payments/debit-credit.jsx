import React, { PropTypes } from 'react';

import Currency from '../../common/currency';
import CurrencyInput from '../../forms/currency-input';
import { Form, FormField } from '../../forms';
import SaveCancel from '../../common/save-cancel';

const DebitCredit = props => {
  const newAvailable = props.availableBalance - props.amountToUse;

  const valueBlock = (label, amount) => {
    return (
      <div className="fc-order-debit-credit__statistic">
        <div className="fc-order-debit-credit__statistic-label">
          {label}
        </div>
        <div className="fc-order-debit-credit__statistic-value">
          <Currency value={amount} />
        </div>
      </div>
    );
  };

  return (
    <Form className="fc-order-debit-credit" onSubmit={props.onSubmit}>
      <div className="fc-order-debit-credit__title">
        {props.title}
      </div>
      <div className="fc-order-debit-credit__form">
        {valueBlock('Available Balance', props.availableBalance)}
        <FormField className="fc-order-debit-credit__amount-form"
                   label="Amount to Use"
                   labelClassName="fc-order-debit-credit__amount-form-value">
          <CurrencyInput onChange={props.onChange}
                         value={props.amountToUse} />
        </FormField>
        {valueBlock('New Available Balance', newAvailable)}
      </div>
      <div className="fc-order-debit-credit__submit">
        <SaveCancel saveText="Add Payment Method"
                    saveDisabled={props.amountToUse == 0} />
      </div>
    </Form>
  );
};

DebitCredit.propTypes = {
  amountToUse: PropTypes.number.isRequired,
  availableBalance: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
};

export default DebitCredit;
