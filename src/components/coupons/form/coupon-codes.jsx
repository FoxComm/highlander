
// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from '../../forms/radio-button';
import { PrimaryButton } from '../../common/buttons';
import { Checkbox } from '../../checkbox/checkbox';
import Counter from '../../forms/counter';
import FormField from '../../forms/formfield';

// styles
import styles from './styles.css';

type Props = {
  onChangeSingleCode: (code: ?string) => any;
  onGenerateBulkCodes: (prefix: string, length: number, quantity: number) => any;
}

export default class CouponCodes extends Component {
  props: Props;

  state = {
    bulk: void 0,
    codesQuantity: 1,
    codesLength: 1,
  };

  get singleCouponFormPart() {
    if (this.state.bulk !== false) {
      return null;
    }

    return (
      <div styleName="form-subset">
        <div className="fc-form-field" styleName="form-group">
          <input
            type="text"
            styleName="full-width-field"
            value={this.state.singleCode}
            onChange={this.handleChangeSingleCode}
          />
          <div styleName="field-comment">
            Coupon codes must be unique and are <i>not</i> case sensative.
          </div>
        </div>
      </div>
    );
  }

  @autobind
  handleChangeSingleCode({target}) {
    this.props.onChangeSingleCode(target.value);
  }

  @autobind
  handleFormChange({target}) {
    this.setFormValue(target.name, target.value);
  }

  @autobind
  handleCounterChange({target}) {
    this.setCounterValue(target.name, target.value);
  }

  @autobind
  setCounterValue(name, value) {
    let num = Number(value);
    num = isNaN(num) ? 1 : num;
    this.setFormValue(name, Math.max(1, num));
  }

  @autobind
  handleGenerateBulkClick() {
    const { codesPrefix, codesLength, codesQuantity } = this.state;
    this.props.onGenerateBulkCodes(codesPrefix, codesLength, codesQuantity);
  }

  setFormValue(name, value) {
    this.setState({
      [name]: value
    });
  }

  get generateCodesDisabled() {
    return !this.state.codesPrefix;
  }

  get bulkCouponFormPart() {
    if (this.state.bulk !== true) {
      return null;
    }

    return (
      <div styleName="form-subset">
        <div styleName="form-group">
          <FormField label="Quantity">
            <div>
              <Counter
                id="codesQuantity"
                name="codesQuantity"
                value={this.state.codesQuantity}
                decreaseAction={() => this.setCounterValue('codesQuantity', this.state.codesQuantity - 1)}
                increaseAction={() => this.setCounterValue('codesQuantity', this.state.codesQuantity + 1)}
                onChange={this.handleCounterChange}
                min={1}
              />
            </div>
          </FormField>
        </div>
        <div styleName="form-group">
          <FormField label="Code Prefix">
            <div>
              <input
                styleName="full-width-field"
                value={this.state.codesPrefix}
                onChange={this.handleFormChange}
                type="text"
                name="codesPrefix"
              />
            </div>
          </FormField>
        </div>
        <div styleName="form-group">
          <FormField label="Code Character Length">
            <div>
              <Counter
                id="codesLength"
                name="codesLength"
                value={this.state.codesLength}
                decreaseAction={() => this.setCounterValue('codesLength', this.state.codesLength - 1)}
                increaseAction={() => this.setCounterValue('codesLength', this.state.codesLength + 1)}
                onChange={this.handleCounterChange}
                min={1}
              />
            </div>
          </FormField>
          <div styleName="field-comment">
            Excludes prefix
          </div>
        </div>
        <div styleName="form-group">
          <Checkbox id="downloadCSVCheckbox">Download a CSV file of the coupon codes</Checkbox>
        </div>
        <div styleName="form-group">
          <Checkbox id="emailCSVCheckbox">Email a CSV file of the coupon codes to other users</Checkbox>
        </div>
        <PrimaryButton
          type="button"
          disabled={this.generateCodesDisabled}
          onClick={this.handleGenerateBulkClick}
        >
          Generate Codes
        </PrimaryButton>
      </div>
    );
  }

  @autobind
  handleSingleSelect() {
    this.setState({bulk: false});
  }

  @autobind
  handleBulkSelect() {
    this.props.onChangeSingleCode(null);
    this.setState({bulk: true});
  }

  render() {
    return (
      <ContentBox title="Coupon Code">
        <div>
          <RadioButton id="singleCouponCodeRadio"
                       checked={this.state.bulk === false}
                       onChange={this.handleSingleSelect} >
            <label htmlFor="singleCouponCodeRadio" styleName="field-label">Single coupon code</label>
          </RadioButton>
        </div>
        {this.singleCouponFormPart}
        <div>
          <RadioButton id="bulkCouponCodeRadio"
                       checked={this.state.bulk === true}
                       onChange={this.handleBulkSelect} >
            <label htmlFor="bulkCouponCodeRadio" styleName="field-label">Bulk generate coupon codes</label>
          </RadioButton>
        </div>
        {this.bulkCouponFormPart}
      </ContentBox>
    );
  }
}
