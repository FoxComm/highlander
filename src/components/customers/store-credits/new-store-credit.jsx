
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { ReasonType } from '../../../lib/reason-utils';
import { codeToName } from '../../../lib/language-utils';
import { transitionTo } from '../../../route-helpers';

// components
import SectionTitle from '../../section-title/section-title';
import FormField from '../../forms/formfield';
import Form from '../../forms/form';
import { Link } from '../../link';
import { PrimaryButton } from '../../common/buttons';
import Dropdown from '../../dropdown/dropdown';
import Currency from '../../common/currency';

// redux
import * as CustomerActions from '../../../modules/customers/details';
import * as NewStoreCreditActions from '../../../modules/customers/new-store-credit';
import * as ScTypesActions from '../../../modules/store-credit-types';
import * as ReasonsActions from '../../../modules/reasons';

// currency - USD only
const currencyList = {
  'USD': 'United States Dollar - USD'
};

function validateCardCode(code) {
  if (!code) {
    return 'cannot be empty';
  }
}

const actions = {
  ...CustomerActions,
  ...NewStoreCreditActions,
  ...ScTypesActions,
  ...ReasonsActions
};

@connect((state, props) => ({
  ...state.customers.details[props.params.customerId],
  ...state.customers.newStoreCredit,
  ...state.storeCreditTypes,
  ...state.reasons
}), actions)
export default class NewStoreCredit extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  static propTypes = {
    params: PropTypes.shape({
      customerId: PropTypes.number.required
    })
  }

  componentDidMount() {
    this.props.fetchCustomer(this.customerId);
    this.props.fetchScTypes();
    this.props.fetchReasons(this.reasonType);
  }

  componentDidUpdate() {
    const reasonId = _.get(this.props, ['reasons', this.reasonType, '0', 'id']);
    if (_.isNumber(reasonId)) {
      this.props.changeScReason(reasonId);
    }
  }

  shouldComponentUpdate(nextProps, nextState) {
    if (_.isNumber(nextProps.form.id)) {
      transitionTo(this.context.history, 'customer-storecredits', {customerId: this.customerId});
      this.props.resetForm();
      return false;
    }
    return true;
  }

  @autobind
  onChangeValue({target}) {
    if (target.name === 'code') {
      this.props.changeGCCode(target.value);
    } else {
      this.props.changeScFormData(target.name, target.value);
    }
  }

  get reasonType() {
    return ReasonType.STORE_CREDIT_CREATION;
  }

  get customerId() {
    return this.props.params.customerId;
  }

  get customerName() {
    const name = _.get(this.props, 'details.name');
    if (name) {
      return `to ${name}`;
    }
  }

  get scTypes() {
    return _.reduce(this.props.types, (acc, type) => {
      acc[type.originType] = codeToName(type.originType);
      return acc;
    }, {});
  }

  get scSubtypes() {
    if (this.props.types && this.props.form.reasonId) {
      const type = _.find(this.props.types, 'originType', this.props.form.type);
      if (type) {
        const subTypes = _.reduce(type.subTypes, (acc, type) => {
          acc[type.originType] = codeToName(type.originType);
          return acc;
        }, {});
        return subTypes;
      }
    }
  }

  get balances() {
    const balances = this.props.balances.map((balance, idx) => {
      const classes = classNames('fc-store-credit-form__balance-value', {
        '_selected': this.props.form.amount === balance
      });
      return (
        <div className={classes}
          key={`balance-${idx}`}
          onClick={() => this.props.changeScFormData('amount', balance)}>
          ${balance/100}
        </div>
      );
    });
    return balances;
  }

  get typeChangeField() {
    return (
      <li className="fc-store-credit-form__input-group">
        <div>
          <label htmlFor="scTypeId" className="fc-store-credit-form__label">
            Store Credit Type
          </label>
        </div>
        <div>
          <Dropdown id="scTypeId"
                    name="type"
                    items={this.scTypes}
                    placeholder="- Select -"
                    value={this.props.form.type}
                    onChange={(value) => this.props.changeScType(value)} />
        </div>
      </li>
    );
  }

  get storeCreditForm() {
    const hiddenClass = classNames('fc-col-md-1-3', {
      '_hidden': _.isEmpty(this.scSubtypes)
    });
    return (
      <Form className="fc-store-credit-form fc-form-vertical"
              onChange={this.onChangeValue}
              onSubmit={() => this.props.createStoreCredit(this.customerId)}>
        <div className="fc-grid">
          <div className="fc-col-md-1-3">
            <ul>
              {this.typeChangeField}
              <li className="fc-store-credit-form__input-group">
                <div>
                  <label htmlFor="scCurrencyField" className="fc-store-credit-form__label">
                    Currency
                  </label>
                </div>
                <div>
                  <Dropdown id="scCurrencyField"
                            name="currency"
                            items={currencyList}
                            placeholder="- Select -"
                            value={this.props.form.currency}
                            onChange={(value) => this.props.changeScFormData('currency', value)} />
                </div>
              </li>
              <li className="fc-store-credit-form__input-group-amount">
                <FormField label="Value"
                           labelClassName="fc-store-credit-form__label">
                  <div className="fc-input-group">
                    <div className="fc-input-prepend"><i className="icon-usd"></i></div>
                    <input id="scAmountField"
                           type="hidden"
                           name="amount"
                           value={this.props.form.amount} />
                    <input id="scAmountField"
                           className="_no-counters"
                           type="number"
                           name="amountText"
                           value={this.props.form.amountText}
                           min="0"
                           step="0.01" />
                  </div>
                </FormField>
              </li>
              <li className="fc-store-credit-form__balances">
                {this.balances}
              </li>
              <li className="fc-store-credit-form__controls">
                <Link to="customer-storecredits"
                      className="fc-btn-link"
                      params={{customerId: this.customerId}} >Cancel</Link>
                <PrimaryButton type="submit" className="fc-store-credit-form__submit">
                  Issue Store Credit
                </PrimaryButton>
              </li>
            </ul>
          </div>
          <div className={hiddenClass} >
            <div>
              <label htmlFor="subReasonIdField" className="fc-store-credit-form__label">
                Subtype
              </label>
            </div>
            <div>
              <Dropdown id="subReasonIdField"
                        name="subReasonId"
                        items={this.scSubtypes}
                        placeholder="- Select -"
                        value={this.props.form.subTypeId}
                        onChange={(value) => this.props.changeScFormData('subTypeId', value)} />
            </div>
          </div>
        </div>
      </Form>
    );
  }

  get giftCardConvertForm() {
    return (
      <Form className="fc-store-credit-form fc-form-vertical"
              onChange={this.onChangeValue}
              onSubmit={() => this.props.convertGiftCard(this.customerId)} >
        <div className="fc-grid">
          <div className="fc-col-md-1-3">
            <ul>
              {this.typeChangeField}
              <li className="fc-store-credit-form__input-group">
                <FormField label="Gift Card Number"
                           labelClassName="fc-store-credit-form__label"
                           validator={validateCardCode}>
                  <input id="gcNumberField"
                         name="code"
                         type="text"
                         placeholder="1111 1111 1111 1111"
                         className="fc-customer-form-input"
                         value={this.props.form.code}
                         formFieldTarget />
                </FormField>
              </li>
              <li className="fc-store-credit-form__input-group">
                <div>
                  <label className="fc-store-credit-form__label">Gift card’s available balance to transfer:</label>
                </div>
                <div className="fc-store-credit-form__gc-value">
                  <Currency value={this.props.form.availableAmount} />
                </div>
              </li>
              <li className="fc-store-credit-form__controls">
                <Link to="customer-storecredits"
                      className="fc-btn-link"
                      params={{customerId: this.customerId}} >Cancel</Link>
                <PrimaryButton type="submit" className="fc-store-credit-form__submit">
                  Transfer Gift Card to Store Credit
                </PrimaryButton>
              </li>
            </ul>
          </div>
        </div>
      </Form>
    );
  }

  render() {
    const form = this.props.form.type === 'giftCardTransfer' ?
      this.giftCardConvertForm : this.storeCreditForm;
    return (
      <div className="fc-store-credits-new">
        <SectionTitle title="Issue New Store Credit" subtitle={this.customerName}/>
        {form}
      </div>
    );
  }
}
