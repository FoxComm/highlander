/* @flow */
import React, { PropTypes, Component } from 'react';
import { autobind } from 'core-decorators';

import Currency from '../../common/currency';
import CurrencyInput from '../../forms/currency-input';
import { Form, FormField } from '../../forms';
import SaveCancel from '../../common/save-cancel';

type Props = {
  amountToUse: number,
  availableBalance: number,
  onCancel: Function,
  onSubmit: Function,
  title: string,
  amountToUse?: number,
  saveText: string,
}

type State = {
  amountToUse: number,
}

export default class DebitCredit extends Component {
  props: Props;

  state: State = {
    amountToUse: this.props.amountToUse || 0,
  };

  static defaultProps = {
    saveText: 'Add Payment Method',
  };

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.amountToUse != this.props.amountToUse) {
      this.setState({
        amountToUse: nextProps.amountToUse,
      });
    }
  }

  get newAvailable(): number {
    return this.props.availableBalance - this.state.amountToUse;
  }

  valueBlock(label: string, amount: number) {
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
  }

  @autobind
  handleAmountToUseChange(value: string) {
    this.setState({
      amountToUse: Math.min(Number(value), this.props.availableBalance),
    });
  }

  @autobind
  handleSubmit(event: SyntheticEvent) {
    event.preventDefault();
    this.props.onSubmit(this.state.amountToUse);
  }

  render() {
    const { props } = this;

    return (
      <Form className="fc-order-debit-credit" onSubmit={this.handleSubmit}>
        <div className="fc-order-debit-credit__title">
          {props.title}
        </div>
        <div className="fc-order-debit-credit__form">
          {this.valueBlock('Available Balance', props.availableBalance)}
          <FormField className="fc-order-debit-credit__amount-form"
                     label="Amount to Use"
                     labelClassName="fc-order-debit-credit__amount-form-value">
            <CurrencyInput
              onChange={this.handleAmountToUseChange}
              value={this.state.amountToUse}
            />
          </FormField>
          {this.valueBlock('New Available Balance', this.newAvailable)}
        </div>
        <div className="fc-order-debit-credit__submit">
          <SaveCancel
            saveText={props.saveText}
            saveDisabled={this.state.amountToUse == 0}
            onCancel={props.onCancel}
          />
        </div>
      </Form>
    );
  }
}
