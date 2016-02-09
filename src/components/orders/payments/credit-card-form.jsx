import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import CreditCardForm from '../../credit-cards/card-form';

class OrderCreditCardForm extends Component {
  static propTypes = {
    card: PropTypes.object.isRequired,
    customerId: PropTypes.number.isRequired,
  };

  render() {
    const props = this.props;
    const blankCreditCard = {
      isDefault: false,
      holderName: '',
      number: '',
      cvv: '',
      expMonth: '',
      expYear: '',
    };

    return (
      <CreditCardForm card={props.card}
                      customerId={props.customerId}
                      form={blankCreditCard}
                      isNew={true} />
    );
  }
}

export default OrderCreditCardForm;

