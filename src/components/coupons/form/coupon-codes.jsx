
/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from '../../forms/radio-button';
import { PrimaryButton } from '../../common/buttons';
import { Checkbox } from '../../checkbox/checkbox';
import Counter from '../../forms/counter';
import FormField from '../../forms/formfield';
import CodeCreationModal from './code-creation-modal';

// styles
import styles from './styles.css';

// redux
import * as actions from '../../../modules/coupons/details';

type Props = {
  onChangeSingleCode: (code: ?string) => any;
  onGenerateBulkCodes: (prefix: string, length: number, quantity: number) => any;
};

type State = {
  codesPrefix: string,
  singleCode: string,
  codesQuantity: number,
  codesLength: number,
  isDialogVisible: boolean,
};

type Target = {
  name: string,
  value: string,
};

class CouponCodes extends Component {
  props: Props;

  state: State = {
    codesPrefix: '',
    singleCode: '',
    codesQuantity: 1,
    codesLength: 1,
    isDialogVisible: false,
  };

  get singleCouponFormPart(): ?Element {
    if (this.props.codeGeneration.bulk !== false) {
      return null;
    }

    return (
      <div styleName="form-subset">
        <div className="fc-form-field" styleName="form-group">
          <input
            type="text"
            styleName="full-width-field"
            value={this.props.codeGeneration.singleCode}
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
  handleChangeSingleCode({target}: {target: Target}): void {
    this.props.couponsGenerationChange(target.name, target.value);
  }

  @autobind
  handleFormChange({target}: {target: Target}): void {
    this.props.couponsGenerationChange(target.name, target.value);
  }

  @autobind
  handleCounterChange({target}: {target: Target}): void {
    this.props.couponsGenerationChange(target.name, target.value);
  }

  @autobind
  setCounterValue(name: string, value: string|number): void {
    let num = Number(value);
    num = isNaN(num) ? 1 : num;
    this.props.couponsGenerationChange(name, Math.max(1, num));
  }

  @autobind
  handleGenerateBulkClick(): void {
    if (this.codeIsOfValidLength()) {
      this.setState({ isDialogVisible: true });
    }
  }

  @autobind
  closeDialog(): void {
    this.setState({ isDialogVisible: false });
  }

  @autobind
  handleConfirmOfCodeGeneration(): void {
    const { codesPrefix, codesLength, codesQuantity } = this.state;
    const nextState = {
      codesPrefix: '',
      codesQuantity: 1,
      codesLength: 1,
      isDialogVisible: false
    };
    this.setState(nextState, () =>
      this.props.onGenerateBulkCodes(codesPrefix, codesLength, codesQuantity)
    );
  }

  codeIsOfValidLength(): boolean {
    const quantity = this.state.codesQuantity;
    const length = this.state.codesLength;
    return length >= Math.ceil(Math.log10(quantity));
  }

  get generateCodesDisabled(): boolean {
    return !(this.state.codesPrefix && this.codeIsOfValidLength());
  }

  get guessProbability(): number {
    const quantity = this.state.codesQuantity;
    const length = this.state.codesLength;
    const numberOfVariants = Math.pow(10, length);
    return Math.round((quantity / numberOfVariants) * 100);
  }

  get codeLengthValidationError(): Element {
    const message =
      'Cannot guarentee uniqueness for the required quantity of codes. Please choose a longer character length.';
    return <div className="fc-form-field-error">{message}</div>;
  }

  get bulkCouponFormPart(): ?Element {
    if (this.props.codeGeneration.bulk !== true) {
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
                value={this.props.codeGeneration.codesQuantity}
                decreaseAction={() => this.setCounterValue('codesQuantity', this.props.codeGeneration.codesQuantity - 1)}
                increaseAction={() => this.setCounterValue('codesQuantity', this.props.codeGeneration.codesQuantity + 1)}
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
                value={this.props.codeGeneration.codesPrefix}
                onChange={this.handleFormChange}
                type="text"
                name="codesPrefix"
              />
            </div>
          </FormField>
        </div>
        <div styleName="form-group">
          <FormField label="Code Character Length" >
            <div>
              <Counter
                id="codesLength"
                name="codesLength"
                value={this.props.codeGeneration.codesLength}
                decreaseAction={() => this.setCounterValue('codesLength', this.props.codeGeneration.codesLength - 1)}
                increaseAction={() => this.setCounterValue('codesLength', this.props.codeGeneration.codesLength + 1)}
                onChange={this.handleCounterChange}
                min={1}
              />
            </div>
          </FormField>
          {!this.codeIsOfValidLength() && this.codeLengthValidationError}
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
          onClick={this.handleGenerateBulkClick} >
          Generate Codes
        </PrimaryButton>
      </div>
    );
  }

  @autobind
  handleSingleSelect(): void {
    this.props.couponsGenerationSelectSingle();
  }

  @autobind
  handleBulkSelect(): void {
    this.props.couponsGenerationSelectBulk();
  }

  render(): Element {
    return (
      <ContentBox title="Coupon Code">
        <div>
          <RadioButton id="singleCouponCodeRadio"
                       checked={this.props.codeGeneration.bulk === false}
                       onChange={this.handleSingleSelect} >
            <label htmlFor="singleCouponCodeRadio" styleName="field-label">Single coupon code</label>
          </RadioButton>
        </div>
        {this.singleCouponFormPart}
        <div>
          <RadioButton id="bulkCouponCodeRadio"
                       checked={this.props.codeGeneration.bulk === true}
                       onChange={this.handleBulkSelect} >
            <label htmlFor="bulkCouponCodeRadio" styleName="field-label">Bulk generate coupon codes</label>
          </RadioButton>
        </div>
        {this.bulkCouponFormPart}
        <CodeCreationModal
          probability={this.guessProbability}
          isVisible={this.state.isDialogVisible}
          cancelAction={this.closeDialog}
          confirmAction={this.handleConfirmOfCodeGeneration}
        />
      </ContentBox>
    );
  }
}

export default connect(null, actions)(CouponCodes);
