// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { createSelector } from 'reselect';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// helpers
import { transitionTo } from 'browserHistory';

// components
import Counter from '../forms/counter';
import Typeahead from '../typeahead/typeahead';
import { Dropdown } from '../dropdown';
import { Form, FormField } from '../forms';
import PilledInput from '../pilled-search/pilled-input';
import SaveCancel from '../common/save-cancel';
import CurrencyInput from '../forms/currency-input';

import { ReasonType } from 'lib/reason-utils';

// redux
import * as GiftCardNewActions from 'modules/gift-cards/new';
import { createGiftCard } from 'modules/gift-cards/list';
import { fetchReasons } from 'modules/reasons';

const typeTitles = {
  'csrAppeasement': 'Appeasement'
};

const reasonType = ReasonType.GIFT_CARD_CREATION;

const subTypes = createSelector(
  ({ giftCards: { adding } }) => adding.giftCard.originType,
  ({ giftCards: { adding } }) => adding.giftCard.types,
  (originType, types = []) => _.get(_.find(types, { originType }), 'subTypes', [])
);

@connect(state => ({
  ...state.giftCards.adding.giftCard,
  subTypes: subTypes(state),
  creationReasons: _.get(state, ['reasons', 'reasons', reasonType], []),
}), {
  ...GiftCardNewActions,
  createGiftCard,
  fetchReasons,
})
export default class NewGiftCard extends React.Component {

  static propTypes = {
    fetchTypes: PropTypes.func,
    balance: PropTypes.number,
    balanceText: PropTypes.string,
    changeFormData: PropTypes.func.isRequired,
    createGiftCard: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    removeCustomer: PropTypes.func,
    subTypes: PropTypes.array,
    types: PropTypes.array,
    changeQuantity: PropTypes.func,
    quantity: PropTypes.number,
    originType: PropTypes.string,
    balances: PropTypes.array,
    fetchReasons: PropTypes.func.isRequired,
  };

  state = {
    customerMessageCount: 0,
    csvMessageCount: 0,
    customersQuery: '',
  };

  componentDidMount() {
    this.props.resetForm();
    if (_.isEmpty(this.props.types)) {
      this.props.fetchTypes();
    }
    if (_.isEmpty(this.props.creationReasons)) {
      this.props.fetchReasons(reasonType).then(() => {
        this.setReason();
      });
    } else {
      this.setReason();
    }
  }

  componentWillUpdate(nextProps) {
    if (this.props.creationReasons != nextProps.creationReasons) {
      this.setReason();
    }
  }

  @autobind
  setReason() {
    const id = _.get(this.props.creationReasons, [0, 'id']);
    this.props.changeFormData('reasonId', id);
  }

  @autobind
  submitForm(event) {
    event.preventDefault();

    this.props.createGiftCard()
      .then(resp => {
        if (!_.isArray(resp) || resp.length === 1) {
          const giftCard = _.get(resp, '[0].giftCard', resp);
          transitionTo('giftcard', { giftCard: giftCard.code });
        } else {
          // TODO: show only created items
          transitionTo('gift-cards');
        }
      });
  }

  @autobind
  onChangeValue({ target }) {
    /** skip balance field handling as it is handled by CurrencyInput change event */
    if (target.name === 'balance') {
      return;
    }

    const value = target.type === 'checkbox' ? target.checked : target.value;

    this.props.changeFormData(target.name, value);
  }

  @autobind
  onChangeAmount(newVal) {
    this.props.changeFormData('balance', Number(newVal));
  }

  get subTypes() {
    const props = this.props;

    if (props.subTypes && props.subTypes.length > 0) {
      return (
        <div className="fc-new-gift-card__subtypes fc-col-md-1-2">
          <label className="fc-new-gift-card__label" htmlFor="subTypeId">Subtype</label>
          <Dropdown value={`${props.subTypeId}`}
                    onChange={ value => props.changeFormData('subTypeId', Number(value)) }
                    items={props.subTypes.map(subType =>[subType.id, subType.title])}
          />
        </div>
      );
    }
  }

  get quantitySection() {
    const changeQuantity = (event, amount) => {
      event.preventDefault();
      this.props.changeQuantity(this.props.quantity + amount);
    };

    return (
      <fieldset className="fc-new-gift-card__fieldset">
        <label className="fc-new-gift-card__label" htmlFor="quantity">Quantity</label>
        <Counter
          id="quantity"
          value={this.props.quantity}
          increaseAction={event => changeQuantity(event, 1)}
          decreaseAction={event => changeQuantity(event, -1)}
          onChange={({target}) => this.props.changeQuantity(target.value)}
          min={1} />
      </fieldset>
    );
  }

  render() {
    const {
      originType,
      changeFormData,
      types,
      balance,
      balances
    } = this.props;

    const saveDisabled = balance === 0;

    return (
      <div className="fc-new-gift-card">
        <header className="fc-col-md-1-1">
          <h1>Issue New Gift Card</h1>
        </header>
        <Form className="fc-form-vertical fc-col-md-1-1"
              onSubmit={this.submitForm}
              onChange={this.onChangeValue}>
          <div className="fc-grid fc-grid-no-gutter fc-new-gift-card__fieldset">
            <div className="fc-new-gift-card__types fc-col-md-1-2">
              <label className="fc-new-gift-card__label" htmlFor="originType">Gift Card Type</label>
              <Dropdown value={originType}
                        onChange={value => changeFormData('originType', value) }
                        items={types.map((entry, idx) => [entry.originType, typeTitles[entry.originType]])}
              />
            </div>
            {this.subTypes}
          </div>
          <fieldset className="fc-new-gift-card__fieldset fc-new-gift-card__amount">
            <label className="fc-new-gift-card__label" htmlFor="value">Value</label>
            <CurrencyInput inputClass="_no-counters"
                           inputName="balance"
                           value={balance}
                           onChange={this.onChangeAmount}
                           step={0.01}
            />
            <div className="fc-new-gift-card__balances">
              {
                balances.map((balance, idx) => {
                  return (
                    <div className={
                          classNames('fc-new-gift-card__balance-value', {
                            '_selected': this.props.balance == balance
                          })
                        }
                         key={`balance-${idx}`}
                         onClick={() => changeFormData('balance', balance)}>
                      ${balance/100}
                    </div>
                  );
                })
              }
            </div>
          </fieldset>
          {this.quantitySection}

          <SaveCancel cancelTo="gift-cards"
                      saveDisabled={saveDisabled}
                      saveText="Issue Gift Card" />
        </Form>
      </div>
    );
  }
}
