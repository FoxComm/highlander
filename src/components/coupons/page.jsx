
/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { push } from 'react-router-redux';

// components
import { PageTitle } from '../section-title';
import { Button } from '../common/buttons';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';
import ButtonWithMenu from '../common/button-with-menu';

// styles
import styles from './form.css';

// redux
import * as CouponActions from '../../modules/coupons/details';

type CouponPageState = {
  coupon: Object,
  couponCode: ?string,
  promotionError: boolean,
};

type CouponPageParams = {
  couponId: string,
};

type CouponPageProps = {
  params: CouponPageParams,
  actions: Object,
  dispatch: Function,
  details: Object,
  children: Element,
  isFetching: boolean,
};

class CouponPage extends Component {

  props: CouponPageProps;

  state: CouponPageState = {
    coupon: this.props.details.coupon,
    promotionError: false,
  };

  componentDidMount(): void {
    if (this.isNew) {
      this.props.actions.couponsNew();
    } else {
      this.props.actions.fetchCoupon(this.entityId);
    }
  }

  componentWillReceiveProps(nextProps: CouponPageProps): void {
    const { isFetching } = nextProps;

    if (!isFetching) {
      const nextCoupon = nextProps.details.coupon;
      if (!nextCoupon) return;

      if (this.isNew && nextCoupon.form.id) {
        this.props.dispatch(push(`/coupons/${nextCoupon.form.id}`));
      }
      if (!this.isNew && !nextCoupon.form.id) {
        this.props.dispatch(push(`/coupons/new`));
      }
      this.setState({ coupon: nextCoupon });
    }
  }

  componentWillUnmount() {
    this.props.actions.couponsNew();
  }

  get entityId(): string|number {
    return this.props.params.couponId;
  }

  get isNew(): boolean {
    return this.entityId === 'new';
  }

  get pageTitle(): string {
    if (this.isNew) {
      return 'New Coupon';
    }

    const { coupon } = this.props.details;
    return _.get(coupon, 'form.attributes.name', '');
  }

  get selectedPromotions(): Array<any> {
    return _.get(this.props, 'details.selectedPromotions', []);
  }

  save(): ?Promise {
    let willBeCoupon = Promise.resolve();

    if (!_.isNumber(this.props.coupon.promotion)) {
      this.setState({promotionError: true});
      return null;
    }

    if (this.props.coupon) {
      const { coupon } = this.props;
      const couponCode = this.props.codeGeneration.singleCode;

      if (this.isNew) {
        willBeCoupon = this.props.actions.createCoupon(coupon);
      } else {
        willBeCoupon = this.props.actions.updateCoupon(coupon);
      }

      if (couponCode != undefined) {
        willBeCoupon.then(() => {
          const couponId = this.state.coupon.id;
          this.props.actions.generateCode(couponId, couponCode);
        });
      }
    }

    return willBeCoupon;
  }

  @autobind
  handleUpdateCoupon(coupon: Object): void {
    this.props.actions.couponsChange(coupon);
  }

  @autobind
  handleGenerateBulkCodes(prefix, length, quantity): void {
    const { coupon } = this.state;

    let willBeCoupon = this.isNew ? this.props.actions.createCoupon(coupon) : Promise.resolve();

    willBeCoupon.then(() => {
      this.props.actions.generateCodes(prefix, length, quantity);
    });
  }

  @autobind
  handleSubmit(): void {
    this.save();
  }

  @autobind
  handleCancel(): void {
    this.props.dispatch(push('/coupons'));
  }

  @autobind
  handleSelectSaving(value) {
    const { actions, dispatch } = this.props;
    const mayBeSaved = this.save();
    if (!mayBeSaved) return;

    mayBeSaved.then(() => {
      switch (value) {
        case 'save_and_new':
          actions.couponsNew();
          break;
        case 'save_and_duplicate':
          dispatch(push(`/coupons/new`));
          break;
        case 'save_and_close':
          dispatch(push(`/coupons`));
          break;
      }
    });
  }

  render(): Element {
    const props = this.props;
    const { coupon, promotionError } = this.state;
    const { codeGeneration } = props;

    if (!coupon || props.isFetching) {
      return <div><WaitAnimation /></div>;
    }

    const children = React.cloneElement(props.children, {
      ...props.children.props,
      coupon,
      promotionError,
      codeGeneration,
      selectedPromotions: this.selectedPromotions,
      onUpdateCoupon: this.handleUpdateCoupon,
      entity: { entityId: this.entityId, entityType: 'coupon' },
    });

    return (
      <div>
        <PageTitle title={this.pageTitle} >
          <Button
            type="button"
            onClick={this.handleCancel}
            styleName="cancel-button">
            Cancel
          </Button>
          <ButtonWithMenu
            title="Save"
            menuPosition="right"
            onPrimaryClick={this.handleSubmit}
            onSelect={this.handleSelectSaving}
            items={[
              ['save_and_new', 'Save and Create New'],
              ['save_and_duplicate', 'Save and Duplicate'],
              ['save_and_close', 'Save and Close'],
            ]}
          />
        </PageTitle>
        <SubNav params={this.props.params} />
        <div styleName="coupon-details">
          {children}
        </div>
      </div>
    );
  }
}

export default connect(
  state => ({
    details: state.coupons.details,
    codeGeneration: state.coupons.details.codeGeneration,
    isFetching: _.get(state.asyncActions, 'getCoupon.inProgress', false),
  }),
  dispatch => ({ actions: bindActionCreators(CouponActions, dispatch), dispatch })
)(CouponPage);
