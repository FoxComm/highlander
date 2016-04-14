
/* @flow */

// libs
import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { pushState } from 'redux-router';
import { transitionTo } from '../../route-helpers';

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

class CouponPage extends Component {

  static propTypes = {
    params: PropTypes.shape({
      couponId: PropTypes.string.isRequired,
    }).isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  state = {
    coupon: this.props.details.coupon,
  };

  componentDidMount(): void {
    if (this.isNew) {
      this.props.actions.couponsNew();
    } else {
      this.props.actions.fetchCoupon(this.entityId);
    }
    this.props.actions.searchCouponPromotions('');
  }

  componentWillReceiveProps(nextProps): void {
    const { isFetching } = nextProps;

    if (!isFetching) {
      const nextCoupon = nextProps.details.coupon;
      if (this.isNew && nextCoupon.form.id) {
        this.props.dispatch(pushState(null, `/coupons/${nextCoupon.form.id}`, ''));
      }
      this.setState({ coupon: nextCoupon });
    }
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

  save(): boolean {
    if (this.state.coupon) {
      const { coupon, couponCode } = this.state;

      let willBeCoupon = Promise.resolve();

      if (this.isNew) {
        willBeCoupon = this.props.actions.createCoupon(coupon);
      } else {
        this.props.actions.updateCoupon(coupon);
      }

      if (couponCode != undefined) {
        willBeCoupon.then(() => {
          this.props.actions.generateCode(coupon.form.id, couponCode);
        });
      }
    }

    return true; // placeholder, change when implementing validation
  }

  @autobind
  handleUpdateCoupon(coupon: Object): void {
    this.setState({ coupon });
  }

  @autobind
  handleUpdateCouponCode(singleCode: string): void {
    this.setState({
      couponCode: singleCode,
    });
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
    transitionTo(this.context.history, 'coupons');
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
          dispatch(pushState(null, `/coupons/new`, ''));
          break;
        case 'save_and_close':
          dispatch(pushState(null, `/coupons`, ''));
          break;
      }
    });
  }

  render(): Element {
    const props = this.props;
    const { coupon } = this.state;

    if (!coupon || props.isFetching) {
      return <div><WaitAnimation /></div>;
    }

    const children = React.cloneElement(props.children, {
      ...props.children.props,
      coupon,
      selectedPromotions: this.selectedPromotions,
      onUpdateCoupon: this.handleUpdateCoupon,
      onUpdateCouponCode: this.handleUpdateCouponCode,
      onGenerateBulkCodes: this.handleGenerateBulkCodes,
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
        <div>
          {children}
        </div>
      </div>
    );
  }
}

export default connect(
  state => ({
    details: state.coupons.details,
    isFetching: _.get(state.asyncActions, 'getCoupon.inProgress', false),
  }),
  dispatch => ({ actions: bindActionCreators(CouponActions, dispatch), dispatch })
)(CouponPage);
