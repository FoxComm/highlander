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

  get selectedPromotions(): Array<any> {
    return _.get(this.props, 'details.selectedPromotions', []);
  }

  save(): ?Promise<*> {
    let willBeCoupon = super.save();

    if (willBeCoupon) {
      const { bulk, singleCode } = this.props.details.codeGeneration;

      if (bulk === false && singleCode != void 0) {
        willBeCoupon.then((data) => {
          const newId = _.get(data, 'id');
          return this.props.actions.generateCode(newId, singleCode);
        }).then(() => {
          this.props.actions.couponsGenerationReset();
        }).then(() => {
          this.props.actions.refresh();
          transitionTo('promotion-coupons',{promotionId: this.props.params.promotionId});
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
  receiveNewObject(nextObject) {
    nextObject.promotion = Number(this.props.params.promotionId);
    nextObject.attributes.name = { // TO BE REMOVED WHEN COUPON NAME WILL BE REMOVED FROM COUPONS SCHEMA
      t: 'string',
      v: 'Coupon name',
    };
    this.setState({
      object: nextObject
    });
  }

  componentDidUpdate(prevProps, prevState) { // CHECK IF NEEDED AFTER KANGAROOS MERGE
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
        saveText="Generate Coupon Code(s)"
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
      this.setState({promotionError: true});
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
    };
  }

  subNav() {
    return null;
  }
}

export default connectPage('coupon', combinedActions)(CouponPage);
