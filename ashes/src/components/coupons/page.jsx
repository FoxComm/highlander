/* @flow weak */

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import { transitionTo } from 'browserHistory';

// components
import { connectPage, ObjectPage } from '../object-page/object-page';
import SaveCancel from 'components/core/save-cancel';

// actions
import * as CouponActions from 'modules/coupons/details';
import { actions } from 'modules/coupons/list';

const refresh = actions.refresh;
const combinedActions = {
  ...CouponActions,
  refresh,
};

type State = {
  promotionError?: boolean,
  object?: Object,
};

type Params = {
  couponId: string,
  promotionId: Number,
  modalCancelAction: Function,
};

type Actions = {
  couponsNew: Function,
  fetchCoupon: () => Promise<*>,
  createCoupon: Function,
  updateCoupon: Function,
  generateCode: Function,
  couponsResetId: Function,
  clearFetchErrors: Function,
  reset: Function,
  codeIsOfValidLength: Function,
  couponsGenerationShowDialog: Function,
  couponsGenerationReset: Function,
  clearSubmitErrors: Function,
  refresh: Function,
};

type Props = {
  params: Params,
  coupon: Object,
  actions: Actions,
  dispatch: Function,
  details: Object,
  children: Element<*>,
  isFetching: boolean,
  isSaving: boolean,
  fetchError: any,
  submitError: any,
  archiveCoupon: Function,
};

class CouponPage extends ObjectPage {
  props: Props;
  state: State;

  constructor(...args) {
    super(...args);
    this.state = {
      ...this.state,
      promotionError: false,
    };
  }

  get unsaved(): boolean {
    return false;
  }

  get selectedPromotions(): Array<any> {
    return _.get(this.props, 'details.selectedPromotions', []);
  }

  @autobind
  updateObjectWithCodes() {
    const { bulk, singleCode, codesQuantity, codesPrefix, codesLength } = this.props.details.codeGeneration;
    const generateCodes = {
      prefix: codesPrefix,
      quantity: codesQuantity,
      length: Number(codesLength) + codesPrefix.length,
    };
    if(!bulk) return {
      ..._.omit(this.state.object,'generateCodes'),
      singleCode,
    };
    return {
      ..._.omit(this.state.object,'singleCode'),
      generateCodes,
    };
  }

  save(): ?Promise<*> {
    this.setState({
      object: this.updateObjectWithCodes(),
    }, () => {
      const { bulk, singleCode } = this.props.details.codeGeneration;
      if (bulk === false && singleCode != void 0) {
        const willBeCoupon = super.save();
        willBeCoupon.then((data) => {
          this.props.actions.couponsGenerationReset();
        }).then(() => {
          this.props.actions.refresh();
          transitionTo('promotion-coupons',{promotionId: this.props.params.promotionId});
        });
      }

      if (bulk === true && this.props.actions.codeIsOfValidLength()) {
        this.props.actions.couponsGenerationShowDialog();
      }
    })
  }

  @autobind
  saveBulk(): Promise<*> {
    const { bulk } = this.props.details.codeGeneration;
    const willBeCoupon = super.save();
    return willBeCoupon;
  }

  @autobind
  receiveNewObject(nextObject) {
    if (_.isArray(nextObject)) return;
    nextObject.promotion = Number(this.props.params.promotionId);
    nextObject.attributes.name = {
      // TO BE REMOVED WHEN COUPON NAME WILL BE REMOVED FROM COUPONS SCHEMA
      t: 'string',
      v: 'Coupon name',
    };
    this.setState({
      object: nextObject,
    });
  }

  componentDidUpdate(prevProps, prevState) {
    // CHECK IF NEEDED AFTER KANGAROOS MERGE
    return;
  }

  @autobind
  titleBar() {
    return;
  }

  @autobind
  alterSave() {
    return (
      <SaveCancel
        onSave={this.handleSubmit}
        cancelDisabled={this.props.isSaving}
        saveDisabled={this.props.isSaving}
        onCancel={this.props.params.modalCancelAction}
        saveLabel="Generate Coupon Code(s)"
      />
    );
  }

  @autobind
  handleUpdateObject(coupon: Object): void {
    let newState: State = {
      object: coupon,
    };
    if (_.isNumber(coupon.promotion)) {
      newState = {
        ...newState,
        promotionError: false,
      };
    }
    this.setState(newState);
  }

  @autobind
  handleCancel(): void {
    this.transitionToList();
    this.props.actions.couponsGenerationReset();
  }

  validateForm() {
    const coupon = this.state.object;
    let formValid = super.validateForm();

    if (coupon && !_.isNumber(coupon.promotion)) {
      this.setState({ promotionError: true });
      formValid = false;
    }

    return formValid;
  }

  @autobind
  createCoupon(): ?Promise<*> {
    if (!this.validateForm()) {
      return null;
    }

    return this.props.actions.createCoupon(this.state.object);
  }

  childrenProps() {
    const props = super.childrenProps();
    return {
      ...props,
      codeGeneration: this.props.details.codeGeneration,
      promotionError: this.state.promotionError,
      createCoupon: this.createCoupon,
      selectedPromotions: this.selectedPromotions,
      refresh: this.props.actions.refresh,
      saveBulk: this.saveBulk,
    };
  }

  subNav() {
    return null;
  }
}

export default connectPage('coupon', combinedActions)(CouponPage);
