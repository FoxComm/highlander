
/* @flow weak */

import _ from 'lodash';
import React, { Component, PropTypes, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { pushState } from 'redux-router';

import styles from './promotion-page.css';

// components
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';
import ErrorAlerts from '../alerts/error-alerts';
import ButtonWithMenu from '../common/button-with-menu';
import { Button } from '../common/buttons';
import Error from '../errors/error';

type Actions = {
  fetchPromotion: Function,
  promotionsNew: Function,
  createPromotion: Function,
  updatePromotion: Function,
  clearSubmitErrors: Function,
};

type Params = {
  promotionId: number,
};

type Details = {
  promotion: Object,
};

type Props = {
  actions: Actions,
  params: Params,
  details: Details,
  submitErrors: Array<any>,
  children: Element,
  fetchError: any,
  isFetching: bool,
  dispatch: Function,
};

import * as PromotionActions from '../../modules/promotions/details';

class PromotionPage extends Component {
  props: Props;

  state = {
    promotion: this.props.details.promotion,
  };

  get entityId() {
    return this.props.params.promotionId;
  }

  get isNew(): boolean {
    return this.entityId === 'new';
  }

  componentDidMount() {
    if (this.isNew) {
      this.props.actions.promotionsNew();
    } else {
      this.props.actions.fetchPromotion(this.entityId);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { isFetching } = nextProps;

    if (!isFetching) {
      const nextPromotion = nextProps.details.promotion;
      if (!nextPromotion) return;

      if (this.isNew && nextPromotion.form.id) {
        this.props.dispatch(pushState(null, `/promotions/${nextPromotion.form.id}`, ''));
      }
      if (!this.isNew && !nextPromotion.form.id) {
        this.props.dispatch(pushState(null, `/promotions/new`, ''));
      }
      this.setState({ promotion: nextProps.details.promotion });
    }
  }

  componentWillUnmount() {
    this.props.actions.promotionsNew();
  }

  get pageTitle(): string {
    if (this.isNew) {
      return 'New Promotion';
    }

    const { promotion } = this.props.details;
    return _.get(promotion, 'form.attributes.name', '');
  }

  @autobind
  handleUpdatePromotion(promotion) {
    this.setState({ promotion });
  }

  save() {
    let mayBeSaved = false;

    if (this.state.promotion) {
      const promotion = this.state.promotion;
      const { form } = this.refs;

      if (form && form.checkValidity) {
        if (!form.checkValidity()) return;
      }

      if (this.isNew) {
        mayBeSaved = this.props.actions.createPromotion(promotion);
      } else {
        mayBeSaved = this.props.actions.updatePromotion(promotion);
      }
    }

    return mayBeSaved;
  }

  @autobind
  handleSubmit() {
    this.save();
  }

  @autobind
  handleSelectSaving(value) {
    const { actions, dispatch } = this.props;
    const mayBeSaved = this.save();
    if (!mayBeSaved) return;

    mayBeSaved.then(() => {
      switch (value) {
        case 'save_and_new':
          actions.promotionsNew();
          break;
        case 'save_and_duplicate':
          dispatch(pushState(null, `/promotions/new`, ''));
          break;
        case 'save_and_close':
          dispatch(pushState(null, `/promotions`, ''));
          break;
      }
    });
  }

  @autobind
  handleCancel(): void {
    this.props.dispatch(pushState(null, '/promotions', ''));
  }

  render(): Element {
    const props = this.props;
    const { promotion } = this.state;
    const { actions } = props;

    if (props.isFetching !== false && !promotion) {
      return <div><WaitAnimation /></div>;
    }

    if (!promotion) {
      return <Error err={props.fetchError} notFound={`There is no promotion with id ${this.entityId}`} />;
    }

    const children = React.cloneElement(props.children, {
      ...props.children.props,
      promotion,
      ref: 'form',
      onUpdatePromotion: this.handleUpdatePromotion,
      entity: { entityId: this.entityId, entityType: 'promotion' },
    });

    return (
      <div>
        <PageTitle title={this.pageTitle}>
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
        <SubNav promotionId={this.entityId} />
        <div styleName="promotion-details">
          <ErrorAlerts errors={this.props.submitErrors} closeAction={actions.clearSubmitErrors} />
          {children}
        </div>
      </div>
    );
  }
}

export default connect(
  state => ({
    details: state.promotions.details,
    isFetching: _.get(state.asyncActions, 'getPromotion.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getPromotion.err', null),
    submitErrors: (
      _.get(state.asyncActions, 'createPromotion.err.messages') ||
      _.get(state.asyncActions, 'updatePromotion.err.messages')
    )
  }),
  dispatch => ({
    actions: bindActionCreators(PromotionActions, dispatch),
    dispatch,
  })
)(PromotionPage);
