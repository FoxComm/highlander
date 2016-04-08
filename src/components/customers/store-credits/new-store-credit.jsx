// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

//helpers
import { ReasonType } from '../../../lib/reason-utils';
import { codeToName } from '../../../lib/language-utils';
import { transitionTo } from '../../../route-helpers';

// components
import { PageTitle } from '../../section-title';
import FormField from '../../forms/formfield';
import Form from '../../forms/form';
import { Link } from '../../link';
import Dropdown from '../../dropdown/dropdown';
import Currency from '../../common/currency';
import SaveCancel from '../../common/save-cancel';

// redux
import * as CustomerActions from '../../../modules/customers/details';
import * as NewStoreCreditActions from '../../../modules/customers/new-store-credit';
import * as ScTypesActions from '../../../modules/store-credit-types';
import * as ReasonsActions from '../../../modules/reasons';

// currency - USD only
const currencyList = [
  ['USD', 'United States Dollar - USD'],
];

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
  };

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
    return _.map(this.props.types, type => [type.originType, codeToName(type.originType)]);
  }

  get scSubtypes() {
    const {types, form} = this.props;

    if (types && form.reasonId) {
      const type = _.find(types, 'originType', form.type);

      if (type) {
        return _.map(type.subTypes, type => [type.id, type.title]);
      }
    }
  }

  get balances() {
    return this.props.balances.map((balance, idx) => {
      const classes = classNames('fc-store-credit-form__balance-value', {
        '_selected': this.props.form.amount === balance
      });

      return (
        <div className={classes}
             key={`balance-${idx}`}
             onClick={() => this.props.changeScFormData('amount', balance)}>
          ${balance / 100}
        </div>
      );
    });
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

    const {form, createStoreCredit, changeScFormData} = this.props;

    return (
      <Form className="fc-store-credit-form fc-form-vertical"
            onChange={this.onChangeValue}
            onSubmit={() => createStoreCredit(this.customerId)}>
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
                            value={form.currency}
                            onChange={(value) => changeScFormData('currency', value)} />
                </div>
              </li>
              <li className="fc-store-credit-form__input-group-amount">
                <FormField label="Value"
                           labelClassName="fc-store-credit-form__label">
                  <div className="fc-input-group fc-store-credit-form__input-group-amount-field">
                    <div className="fc-input-prepend fc-store-credit-form__amount-field-prepend">
                      <i className="icon-usd"></i>
                    </div>
                    <input id="scAmountField"
                           type="hidden"
                           name="amount"
                           value={form.amount} />
                    <input id="scAmountTextField"
                           className="fc-store-credit-form__amount-field _no-counters"
                           type="number"
                           name="amountText"
                           value={form.amountText}
                           min="0"
                           step="0.01" />
                  </div>
                </FormField>
              </li>
              <li className="fc-store-credit-form__balances">
                {this.balances}
              </li>
              <li className="fc-store-credit-form__controls">
                <SaveCancel cancelTo="customer-storecredits"
                            cancelParams={{customerId: this.customerId}}
                            saveText="Issue Store Credit" />
              </li>
            </ul>
          </div>
          <div className={hiddenClass}>
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
                        value={form.subTypeId}
                        onChange={(value) => changeScFormData('subTypeId', value)} />
            </div>
          </div>
        </div>
      </Form>
    );
  }

  get giftCardConvertForm() {
    const {form, convertGiftCard} = this.props;

    return (
      <Form className="fc-store-credit-form fc-form-vertical"
            onChange={this.onChangeValue}
            onSubmit={() => convertGiftCard(this.customerId)}>
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
                         value={form.code}
                         formFieldTarget />
                </FormField>
              </li>
              <li className="fc-store-credit-form__input-group">
                <div>
                  <label className="fc-store-credit-form__label">Gift card’s available balance to transfer:</label>
                </div>
                <div className="fc-store-credit-form__gc-value">
                  <Currency value={form.availableAmount} />
                </div>
              </li>
              <li className="fc-store-credit-form__controls">
                <SaveCancel cancelTo="customer-storecredits"
                            cancelParams={{customerId: this.customerId}}
                            saveText="Transfer Gift Card to Store Credit" />
              </li>
            </ul>
          </div>
        </div>
      </Form>
    );
  }

  render() {
    const form = this.props.form.type === 'giftCardTransfer'
      ? this.giftCardConvertForm
      : this.storeCreditForm;

    return (
      <div className="fc-store-credits-new">
        <PageTitle title="Issue New Store Credit" subtitle={this.customerName}/>
        {form}
      </div>
    );
  }
}
