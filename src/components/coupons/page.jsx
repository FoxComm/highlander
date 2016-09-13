/* @flow weak */

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { connectPage, ObjectPage } from '../object-page/object-page';
import SubNav from './sub-nav';

// actions
import * as CouponActions from '../../modules/coupons/details';

type State = {
  promotionError?: boolean,
  entity?: Object,
};

type Params = {
  couponId: string,
};

type Actions = {
  couponsNew: Function,
  fetchCoupon: () => Promise,
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
};

type Props = {
  params: Params,
  coupon: Object,
  actions: Actions,
  dispatch: Function,
  details: Object,
  children: Element,
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

  get selectedPromotions(): Array<any> {
    return _.get(this.props, 'details.selectedPromotions', []);
  }

  save(): ?Promise {
    let willBeCoupon = super.save();

    if (willBeCoupon) {
      const { bulk, singleCode } = this.props.details.codeGeneration;

      if (bulk === false && singleCode != void 0) {
        willBeCoupon.then((data) => {
          const newId = _.get(data, ['payload', 'id']);
          return this.props.actions.generateCode(newId, singleCode);
        }).then(() => {
          this.props.actions.couponsGenerationReset();
        });
      }

      if (bulk === true && this.props.actions.codeIsOfValidLength()) {
        willBeCoupon.then(() => {
          return this.props.actions.couponsGenerationShowDialog();
        });
      }
    }

    return willBeCoupon;
  }

  @autobind
  handleUpdateEntity(coupon: Object): void {
    let newState: State = {
      entity: coupon,
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
    const coupon = this.state.entity;
    let formValid = super.validateForm();

    if (coupon && !_.isNumber(coupon.promotion)) {
      this.setState({promotionError: true});
      formValid = false;
    }

    return formValid;
  }

  @autobind
  createCoupon(): ?Promise {
    if (!this.validateForm()) {
      return null;
    }

    return this.props.actions.createCoupon(this.state.entity);
  }

  childrenProps() {
    const props = super.childrenProps();
    return {
      ...props,
      codeGeneration: this.props.details.codeGeneration,
      promotionError: this.state.promotionError,
      createCoupon: this.createCoupon,
      selectedPromotions: this.selectedPromotions,
    };
  }

  subNav() {
    return <SubNav params={this.props.params} />;
  }
}

export default connectPage('coupon', CouponActions)(CouponPage);
