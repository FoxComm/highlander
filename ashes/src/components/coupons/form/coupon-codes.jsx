
/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from '../../forms/radio-button';
import Counter from '../../forms/counter';
import FormField from '../../forms/formfield';
import CodeCreationModal from './code-creation-modal';
import { transitionTo } from 'browserHistory';
import TextInput from 'components/core/text-input';

// styles
import styles from './styles.css';

// redux
import * as actions from 'modules/coupons/details';

type Props = {
  isNew: boolean,
  isValid: boolean,
  refresh: Function,
  codeGeneration: Object,
  promotionId: Number,
  coupon: Object,
  createCoupon: Function,
  couponsGenerationSelectBulk: Function,
  couponsGenerationSelectSingle: Function,
  generateCodes: Function,
  persistCoupon: Function,
  couponsGenerationShowDialog: Function,
  couponsGenerationHideDialog: Function,
  couponsGenerationChange: Function,
  couponsGenerationReset: Function,
  codeIsOfValidLength: Function,
};

type Target = {
  name: string,
  value: string,
};

class CouponCodes extends Component {
  props: Props;

  get singleCouponFormPart() {
    if (this.props.codeGeneration.bulk !== false) {
      return null;
    }

    return (
      <div styleName="form-subset">
        <div className="fc-form-field" styleName="form-group">
          <TextInput
            type="text"
            styleName="full-width-field"
            name="singleCode"
            value={this.props.codeGeneration.singleCode}
            onChange={this.handleFormChange}
          />
          <div styleName="field-comment">
            Coupon codes must be unique and are <i>not</i> case sensative.
          </div>
        </div>
      </div>
    );
  }

  @autobind
  handleFormChange(target: Target): void {
    this.props.couponsGenerationChange(target.name, target.value);
  }

  @autobind
  handleCounterChange({target}: {target: Target}): void {
    const num = Number(target.value);
    this.props.couponsGenerationChange(target.name, num);
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
      let willBeCoupon = this.props.isNew ? this.props.createCoupon() : Promise.resolve();

      if (willBeCoupon == null) return;

      willBeCoupon.then(() => {
        this.props.couponsGenerationShowDialog();
      });
    }
  }

  @autobind
  closeDialog(): void {
    this.props.couponsGenerationHideDialog();
  }

  @autobind
  handleConfirmOfCodeGeneration(): void {
    const { codesPrefix, codesLength, codesQuantity } = this.props.codeGeneration;

    this.props.generateCodes(codesPrefix, codesLength, codesQuantity).then(() => {
      this.props.couponsGenerationReset();
    }).then(() => {
      this.props.refresh();
      transitionTo('promotion-coupons', {promotionId: this.props.promotionId});
    });
  }

  codeIsOfValidLength(): boolean {
    return this.props.codeIsOfValidLength();
  }

  get generateCodesDisabled(): boolean {
    return !(this.props.codeGeneration.codesPrefix && this.codeIsOfValidLength());
  }

  get guessProbability(): number {
    const quantity = this.props.codeGeneration.codesQuantity;
    const length = this.props.codeGeneration.codesLength;
    const numberOfVariants = Math.pow(10, length);
    return Math.round((quantity / numberOfVariants) * 100);
  }

  get codeLengthValidationError() {
    const message =
      'Cannot guarantee uniqueness for the required quantity of codes. Please choose a longer character length.';
    return <div className="fc-form-field-error">{message}</div>;
  }

  get bulkCouponFormPart() {
    if (this.props.codeGeneration.bulk !== true) {
      return null;
    }

    const { codesQuantity } = this.props.codeGeneration;

    return (
      <div styleName="form-subset">
        <div styleName="form-group" className="fc-coupon-inline-row">
          <FormField label="Quantity">
            <div>
              <Counter
                counterId="fct-codes-quantity-counter"
                id="codesQuantity"
                name="codesQuantity"
                value={codesQuantity}
                decreaseAction={() => this.setCounterValue('codesQuantity', codesQuantity - 1)}
                increaseAction={() => this.setCounterValue('codesQuantity', codesQuantity + 1)}
                onChange={this.handleCounterChange}
                min={1}
              />
            </div>
          </FormField>
        </div>
        <div styleName="form-group" className="fc-coupon-inline-row">
          <FormField label="Code Prefix">
            <div>
              <TextInput
                styleName="full-width-field"
                value={this.props.codeGeneration.codesPrefix}
                onChange={this.handleFormChange}
                type="text"
                name="codesPrefix"
              />
            </div>
          </FormField>
        </div>
        <div styleName="form-group" className="fc-coupon-inline-row">
          <FormField label="Code Character Length" >
            <div>
              <Counter
                counterId="fct-code-length-counter"
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

  render() {
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
          isVisible={this.props.codeGeneration.isDialogVisible}
          cancelAction={this.closeDialog}
          confirmAction={this.handleConfirmOfCodeGeneration}
        />
      </ContentBox>
    );
  }
}

const mergeProps = (stateProps, dispatchProps, ownProps) => {
  return {
    ...dispatchProps,
    ...ownProps
  };
};

export default connect(null, actions, mergeProps)(CouponCodes);
