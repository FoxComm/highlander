import _ from 'lodash';
import { connect } from 'react-redux';
import React, { Component } from 'react';

import styles from './checkout.css';

import localized from 'lib/i18n';

import Checkbox from 'ui/checkbox';
import Loader from 'ui/loader';
import Icon from 'ui/icon';

import { fetchCreditCards } from 'modules/checkout';

function mapStateToProps(state) {
  return {
    isLoading: _.get(state.asyncActions, ['creditCards', 'inProgress'], true),
    creditCards: state.checkout.creditCards,
    selectedCreditCard: state.cart.creditCard,
  };
}

/* ::`*/
@connect(mapStateToProps, { fetchCreditCards })
@localized
  /* ::`*/
class CreditCards extends Component {

  componentWillMount() {
    this.props.fetchCreditCards();
  }

  render() {
    const { isLoading, creditCards, selectedCreditCard, selectCreditCard, t } = this.props;

    if (isLoading) {
      return <Loader size="m" />;
    }

    if (_.isEmpty(creditCards)) {
      return <div styleName="credit-cards-empty">{t('No credit cards yet')}</div>;
    }

    return (
      <div>
        {creditCards.map(creditCard => {
          const brand = creditCard.brand ? creditCard.brand.toLowerCase() : '';

          return (
            <div key={creditCard.id} styleName="credit-card">
              <Checkbox
                name="credit-card"
                checked={selectedCreditCard && selectedCreditCard.id == creditCard.id}
                onChange={() => selectCreditCard(creditCard)}
                id={`credit-card-${creditCard.id}`}
              >
                <span>
                  <span>•••• {creditCard.lastFour}</span>
                  <span styleName="credit-card-valid">
                    {creditCard.expMonth}/{creditCard.expYear.toString().slice(-2)}
                  </span>
                </span>
              </Checkbox>
              <Icon styleName="payment-icon" name={`fc-payment-${brand}`} />
            </div>
          );
        })}
      </div>
    );
  }
}

export default localized(CreditCards);
