
// libs
import _ from 'lodash';
import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { pushState } from 'redux-router';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton, Button } from '../common/buttons';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';

// redux
import * as CouponActions from '../../modules/coupons/details';

class CouponPage extends Component {

  static propTypes = {
    params: PropTypes.shape({
      couponId: PropTypes.string.isRequired,
    }).isRequired,
  };

  state = {
    coupon: this.props.details.coupon,
  };

  componentDidMount() {
    if (this.isNew) {
      this.props.actions.couponsNew();
    } else {
      this.props.actions.fetchCoupon(this.entityId);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { isFetching } = nextProps;

    if (!isFetching) {
      const nextCoupon = nextProps.details.coupon;
      console.log(this.isNew);
      console.log(nextCoupon.form.id);
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

  @autobind
  handleUpdateCoupon(coupon) {
    this.setState({ coupon });
  }

  @autobind
  handleUpdateCouponCode(singleCode) {
    this.setState({
      couponCode: singleCode,
    });
  }

  @autobind
  handleGenerateBulkCodes(prefix, length, quantity) {
    const { coupon } = this.state;

    let willBeCoupon = this.isNew ? this.props.actions.createCoupon(coupon) : Promise.resolve();

    willBeCoupon.then(() => {
      this.props.actions.generateCodes(prefix, length, quantity);
    });
  }

  @autobind
  handleSubmit() {
    if (this.state.coupon) {
      const { coupon, couponCode } = this.state;

      let willBeCoupon = Promise.resolve();

      if (this.isNew) {
        willBeCoupon = this.props.actions.createCoupon(coupon);
      } else {
        this.props.actions.updateCoupon(coupon);
      }

      willBeCoupon.then(() => {
        this.props.actions.generateCode(coupon.form.id, couponCode);
      });
    }
  }

  render() {
    const props = this.props;
    const { coupon } = this.state;

    if (!coupon || props.isFetching) {
      return <div><WaitAnimation /></div>;
    }

    const children = React.cloneElement(props.children, {
      ...props.children.props,
      coupon,
      onUpdateCoupon: this.handleUpdateCoupon,
      onUpdateCouponCode: this.handleUpdateCouponCode,
      onGenerateBulkCodes: this.handleGenerateBulkCodes,
      entity: { entityId: this.entityId, entityType: 'coupon' },
    });

    return (
      <div>
        <PageTitle title={this.pageTitle} >
          <Button>
            Cancel
          </Button>
          <PrimaryButton
            type="submit"
            onClick={this.handleSubmit} >
            Save
          </PrimaryButton>
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
