// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { createSelector } from 'reselect';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// helpers
import { transitionTo } from '../../route-helpers';

// components
import Counter from '../forms/counter';
import PrependInput from '../forms/prepend-input';
import Typeahead from '../typeahead/typeahead';
import { Dropdown, DropdownItem } from '../dropdown';
import { Checkbox } from '../checkbox/checkbox';
import { Form, FormField } from '../forms';
import ChooseCustomers from './choose-customers';
import PilledInput from '../pilled-search/pilled-input';
import SaveCancel from '../common/save-cancel';

// redux
import * as GiftCardNewActions from '../../modules/gift-cards/new';
import { createGiftCard } from '../../modules/gift-cards/list';

const typeTitles = {
  'csrAppeasement': 'Appeasement'
};

const subTypes = createSelector(
  ({giftCards: {adding}}) => adding.originType,
  ({giftCards: {adding}}) => adding.types,
  (originType, types=[]) => _.get(_.findWhere(types, {originType}), 'subTypes', [])
);

@connect(state => ({
  ...state.giftCards.adding,
  subTypes: subTypes(state)
}), {
  ...GiftCardNewActions,
  createGiftCard
})
export default class NewGiftCard extends React.Component {

  static propTypes = {
    addCustomers: PropTypes.func,
    fetchTypes: PropTypes.func,
    balance: PropTypes.number,
    balanceText: PropTypes.string,
    changeFormData: PropTypes.func.isRequired,
    createGiftCard: PropTypes.func.isRequired,
    customers: PropTypes.array,
    emailCSV: PropTypes.bool,
    removeCustomer: PropTypes.func,
    sendToCustomer: PropTypes.bool,
    subTypes: PropTypes.array,
    suggestCustomers: PropTypes.func,
    suggestedCustomers: PropTypes.array,
    types: PropTypes.array,
    changeQuantity: PropTypes.func,
    quantity: PropTypes.number
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      customerMessageCount: 0,
      csvMessageCount: 0,
      customersQuery: '',
    };
  }

  componentDidMount() {
    this.props.fetchTypes();
  }

  @autobind
  submitForm(event) {
    event.preventDefault();
    this.props.createGiftCard()
      .then(() => transitionTo(this.context.history, 'gift-cards'));
  }

  @autobind
  onChangeValue({target}) {
    const value = target.type === 'checkbox' ? target.checked : target.value;

    this.props.changeFormData(target.name, value);
  }

  changeCustomerMessage(event) {
    this.setState({customerMessageCount: event.target.value.length});
  }

  get subTypes() {
    const props = this.props;

    if (props.subTypes && props.subTypes.length > 0) {
      return (
        <div className="fc-new-gift-card__subtypes fc-col-md-1-2">
          <label className="fc-new-gift-card__label" htmlFor="subTypeId">Subtype</label>
          <Dropdown value={`${props.subTypeId}`} onChange={ value => props.changeFormData('subTypeId', Number(value)) }>
            {props.subTypes.map((subType, idx) => {
              return <DropdownItem key={`subType-${subType.id}`} value={`${subType.id}`}>{subType.title}</DropdownItem>;
            })}
          </Dropdown>
        </div>
      );
    }
  }

  get chooseCustomersMenu() {
    return (
      <ChooseCustomers
        items={this.props.suggestedCustomers}
        onAddCustomers={(customers) => {
          this.props.addCustomers(_.values(customers));
          this.setState({
            customersQuery: ''
          });
        }} />
    );
  }

  get chooseCustomersInput() {
    const {customers, removeCustomer} = this.props;

    return (
      <PilledInput
        solid={true}
        value={this.state.customersQuery}
        onChange={e => this.setState({customersQuery: e.target.value})}
        pills={customers.map(customer => customer.name)}
        icon={null}
        onPillClose={(name, idx) => removeCustomer(customers[idx].id)} />
    );
  }

  get customerListBlock() {
    const props = this.props;

    if (props.sendToCustomer) {
      const labelAtRight = <div className="fc-new-gift-card__counter">{this.state.customerMessageCount}/1000</div>;

      return (
        <div className="fc-new-gift-card__send-to-customers">
          <Typeahead
            className="_no-search-icon"
            fetchItems={props.suggestCustomers}
            itemsElement={this.chooseCustomersMenu}
            inputElement={this.chooseCustomersInput}
            minQueryLength={2}
            label="Choose customers:"
            name="customerQuery" />
          <FormField className="fc-new-gift-card__message-to-customers"
                     label="Write a message for customers" optional
                     labelAtRight={ labelAtRight }
                     labelClassName="fc-new-gift-card__label">
            <textarea className="fc-input" name="customerMessage"
                      maxLength="1000" onChange={this.changeCustomerMessage.bind(this)}></textarea>
          </FormField>
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
      balanceText,
      sendToCustomer,
      customers
    } = this.props;

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
              <Dropdown value={originType} onChange={value => changeFormData('originType', value) }>
                {types.map((entry, idx) => {
                  const type = entry.originType;
                  const title = typeTitles[entry.originType];

                  return <DropdownItem value={type} key={`${idx}-${type}`}>{title}</DropdownItem>;
                })}
              </Dropdown>
            </div>
            {this.subTypes}
          </div>
          <fieldset className="fc-new-gift-card__fieldset fc-new-gift-card__amount">
            <label className="fc-new-gift-card__label" htmlFor="value">Value</label>
            <PrependInput
              icon="usd"
              inputClass="_no-counters"
              inputName="balance"
              inputNamePretty="balanceText"
              inputType="number"
              inputValue={balance}
              inputValuePretty={balanceText}
              step="0.01"
              min="1" />
            <div className="fc-new-gift-card__balances">
              {
                [1000, 2500, 5000, 10000, 20000].map((balance, idx) => {
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
          <fieldset className="fc-new-gift-card__fieldset">
            <label className="fc-new-gift-card__label">
              <Checkbox id="sendToCustomer" name="sendToCustomer" checked={sendToCustomer} />
              Send gift card(s) to customer(s)
            </label>
            { this.customerListBlock }
          </fieldset>
          <SaveCancel cancelTo="gift-cards"
                      saveDisabled={sendToCustomer && customers.length === 0}
                      saveText="Issue Gift Card" />
        </Form>
      </div>
    );
  }
}
