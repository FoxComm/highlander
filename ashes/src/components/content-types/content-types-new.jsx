// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { createSelector } from 'reselect';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// helpers
import { transitionTo, transitionToLazy } from 'browserHistory';

// components
import Counter from '../forms/counter';
import { Dropdown } from '../dropdown';
import { Form } from '../forms';
import SaveCancel from 'components/core/save-cancel';
import CurrencyInput from '../forms/currency-input';

import { ReasonType } from 'lib/reason-utils';

// redux
import * as ContentTypeNewActions from 'modules/content-types/new';
import { createContentType } from 'modules/content-types/list';
import { fetchReasons } from 'modules/reasons';

const typeTitles = {
  'csrAppeasement': 'Appeasement'
};

const reasonType = ReasonType.CONTENT_TYPE_CREATION;

const subTypes = createSelector(
  ({ contentTypes: { adding } }) => adding.contentType.originType,
  ({ contentTypes: { adding } }) => adding.contentType.types,
  (originType, types = []) => _.get(_.find(types, { originType }), 'subTypes', [])
);

@connect(state => ({
  ...state.contentTypes.adding.contentType,
  subTypes: subTypes(state),
  creationReasons: _.get(state, ['reasons', 'reasons', reasonType], []),
}), {
  ...ContentTypeNewActions,
  createContentType,
  fetchReasons,
})
export default class NewContentType extends React.Component {

  static propTypes = {
    fetchTypes: PropTypes.func,
    balance: PropTypes.number,
    balanceText: PropTypes.string,
    changeFormData: PropTypes.func.isRequired,
    createContentType: PropTypes.func.isRequired,
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

    this.props.createContentType()
      .then(resp => {
        if (!_.isArray(resp) || resp.length === 1) {
          const contentType = _.get(resp, '[0].contentType', resp);
          transitionTo('contenttype', { contentType: contentType.code });
        } else {
          // TODO: show only created items
          transitionTo('content-types');
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
        <div className="fc-new-content-type__subtypes fc-col-md-1-2">
          <label className="fc-new-content-type__label" htmlFor="subTypeId">Subtype</label>
          <Dropdown id="content-type-subtype-dd"
                    value={`${props.subTypeId}`}
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
      <fieldset className="fc-new-content-type__fieldset">
        <label className="fc-new-content-type__label" htmlFor="quantity">Quantity</label>
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
      <div className="fc-new-content-type">
        <header className="fc-col-md-1-1">
          <h1>Issue New Content Type</h1>
        </header>
        <Form className="fc-form-vertical fc-col-md-1-1"
              onSubmit={this.submitForm}
              onChange={this.onChangeValue}>
          <div className="fc-grid fc-grid-no-gutter fc-new-content-type__fieldset">
            <div className="fc-new-content-type__types fc-col-md-1-2">
              <label className="fc-new-content-type__label" htmlFor="originType">Content Type Type</label>
              <Dropdown id="fct-content-type-type-dd"
                        value={originType}
                        onChange={value => changeFormData('originType', value) }
                        items={types.map((entry, idx) => [entry.originType, typeTitles[entry.originType]])}
              />
            </div>
            {this.subTypes}
          </div>
          <fieldset className="fc-new-content-type__fieldset fc-new-content-type__amount">
            <label className="fc-new-content-type__label" htmlFor="value">Value</label>
            <CurrencyInput inputClass="_no-counters"
                           inputName="balance"
                           value={balance}
                           onChange={this.onChangeAmount}
                           step={0.01}
            />
            <div className="fc-new-content-type__balances">
              {
                balances.map((balance, idx) => {
                  return (
                    <div className={
                          classNames('fc-new-content-type__balance-value', {
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

          <SaveCancel
            onCancel={transitionToLazy('content-types')}
            saveDisabled={saveDisabled}
            saveText="Issue Content Type"
          />
        </Form>
      </div>
    );
  }
}
