/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from 'components/core/radio-button';
import Counter from 'components/core/counter';
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
  handleFormChange(value: string, name: string): void {
    this.props.couponsGenerationChange(name, value);
  }

  @autobind
  handleCounterChange(name, num): void {
    this.props.couponsGenerationChange(name, num);
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

    this.props
      .generateCodes(codesPrefix, codesLength, codesQuantity)
      .then(() => {
        this.props.couponsGenerationReset();
      })
      .then(() => {
        this.props.refresh();
        transitionTo('promotion-coupons', { promotionId: this.props.promotionId });
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
    return Math.round(quantity / numberOfVariants * 100);
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
                onChange={value => this.handleCounterChange('codesQuantity', value)}
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
                name="codesPrefix"
              />
            </div>
          </FormField>
        </div>
        <div styleName="form-group" className="fc-coupon-inline-row">
          <FormField label="Code Character Length">
            <div>
              <Counter
                counterId="fct-code-length-counter"
                id="codesLength"
                name="codesLength"
                value={this.props.codeGeneration.codesLength}
                onChange={value => this.handleCounterChange('codesLength', value)}
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
          <RadioButton
            id="singleCouponCodeRadio"
            label="Single coupon code"
            checked={this.props.codeGeneration.bulk === false}
            onChange={this.handleSingleSelect}
          />
        </div>
        {this.singleCouponFormPart}
        <div>
          <RadioButton
            id="bulkCouponCodeRadio"
            label="Bulk generate coupon codes"
            checked={this.props.codeGeneration.bulk === true}
            onChange={this.handleBulkSelect}
          />
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
    ...ownProps,
  };
};

export default connect(null, actions, mergeProps)(CouponCodes);
