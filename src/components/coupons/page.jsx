/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
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
import ErrorAlerts from '../alerts/error-alerts';
import Error from '../errors/error';
import ArchiveActionsSection from '../archive-actions/archive-actions';

// styles
import styles from './form.css';

// actions
import * as CouponActions from '../../modules/coupons/details';
import * as ArchiveActions from '../../modules/coupons/archive';

//helpers
import { isArchived } from 'paragons/common';
import { transitionTo } from 'browserHistory';
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';

type State = {
  promotionError: boolean,
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
  couponsChange: Function,
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
  codeGeneration: Object,
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

class CouponPage extends Component {
  props: Props;

  state: State = {
    promotionError: false,
  };

  componentDidMount(): void {
    this.props.actions.clearFetchErrors();
    if (this.isNew) {
      this.props.actions.couponsNew();
    } else {
      this.props.actions.fetchCoupon(this.entityId)
        .then(({payload}) => {
          if (isArchived(payload)) transitionTo('coupons');
        });
    }
  }

  componentWillReceiveProps(nextProps: Props): void {
    const { isFetching } = nextProps;

    if (!isFetching) {
      const nextCoupon = nextProps.details.coupon;
      if (!nextCoupon || _.isEqual(nextCoupon, this.coupon)) return;

      if (this.isNew && nextCoupon.form.id) {
        this.props.dispatch(push(`/coupons/${nextCoupon.form.id}`));
      }
      if (!this.isNew && !nextCoupon.form.id) {
        this.props.dispatch(push(`/coupons/new`));
      }
    }
  }

  componentWillUnmount() {
    this.props.actions.reset();
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

  get coupon(): Object {
    return this.props.details.coupon;
  }

  save(): ?Promise {
    let willBeCoupon = Promise.resolve();

    const coupon = this.coupon;

    if (!this.validateForm()) {
      return null;
    }

    if (coupon) {
      if (this.isNew) {
        willBeCoupon = this.props.actions.createCoupon(coupon);
      } else {
        willBeCoupon = this.props.actions.updateCoupon(coupon);
      }

      const { bulk, singleCode, codesPrefix, codesLength, codesQuantity } = this.props.codeGeneration;

      if (bulk === false && singleCode != undefined) {
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
  handleUpdateCoupon(coupon: Object): void {
    let errors = {};
    if (_.isNumber(coupon.promotion)) {
      errors = { promotionError: false };
    }
    this.setState(errors, () => {
      this.props.actions.couponsChange(coupon);
    });
  }

  @autobind
  handleSubmit(): void {
    this.save();
  }

  @autobind
  handleCancel(): void {
    this.props.dispatch(push('/coupons'));
    this.props.actions.couponsGenerationReset();
  }

  @autobind
  handleSelectSaving(value: string): void {
    const { actions, dispatch } = this.props;
    const mayBeSaved = this.save();
    if (!mayBeSaved) return;

    mayBeSaved.then(() => {
      switch (value) {
        case SAVE_COMBO.NEW:
          actions.couponsNew();
          break;
        case SAVE_COMBO.DUPLICATE:
          this.props.actions.couponsResetId();
          dispatch(push(`/coupons/new`));
          break;
        case SAVE_COMBO.CLOSE:
          dispatch(push(`/coupons`));
          break;
      }
    });
  }

  validateForm() {
    const { form } = this.refs;
    const coupon = this.coupon;
    let formValid = true;

    if (!_.isNumber(coupon.promotion)) {
      this.setState({promotionError: true});
      formValid = false;
    }

    if (form && form.checkValidity && !form.checkValidity()) {
      formValid = false;
    }

    return formValid;
  }

  @autobind
  handleSave(): ?Promise {
    if (!this.validateForm()) {
      return null;
    }

    return this.props.actions.createCoupon(this.coupon);
  }

  renderArchiveActions() {
    return(
      <ArchiveActionsSection type="Coupon"
                             title={this.pageTitle}
                             archive={this.archiveCoupon} />
    );
  }

  @autobind
  archiveCoupon() {
    this.props.archiveCoupon(this.props.params.couponId).then(() => {
      transitionTo('coupons');
    });
  }

  render(): Element {
    const props = this.props;
    const coupon = this.coupon;
    const { promotionError } = this.state;
    const { codeGeneration, actions } = props;

    if (props.isFetching !== false && !coupon) {
      return <div><WaitAnimation /></div>;
    }

    if (!coupon) {
      return <Error err={props.fetchError} notFound={`There is no coupon with id ${this.entityId}`} />;
    }

    const children = React.cloneElement(props.children, {
      ...props.children.props,
      coupon,
      promotionError,
      codeGeneration,
      ref: 'form',
      saveCoupon: this.handleSave,
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
            isLoading={props.isSaving}
            items={SAVE_COMBO_ITEMS}
          />
        </PageTitle>
        <SubNav params={this.props.params} />
        <div styleName="coupon-details">
          <ErrorAlerts error={this.props.submitError} closeAction={actions.clearSubmitErrors} />
          {children}
        </div>

        {!this.isNew && this.renderArchiveActions()}
      </div>
    );
  }
}

export default connect(
  state => ({
    details: state.coupons.details,
    codeGeneration: state.coupons.details.codeGeneration,
    isFetching: _.get(state.asyncActions, 'getCoupon.inProgress', null),
    isSaving: (
      _.get(state.asyncActions, 'createCoupon.inProgress', false)
      || _.get(state.asyncActions, 'updateCoupon.inProgress', false)
    ),
    fetchError: _.get(state.asyncActions, 'getCoupon.err', null),
    submitError: (
      _.get(state.asyncActions, 'createCoupon.err') ||
      _.get(state.asyncActions, 'updateCoupon.err')
    )
  }),
  dispatch => ({
    actions: bindActionCreators(CouponActions, dispatch),
    ...bindActionCreators(ArchiveActions, dispatch),
    dispatch
  })
)(CouponPage);
