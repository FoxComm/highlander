/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { push } from 'react-router-redux';

// styles
import styles from './promotion-page.css';

// components
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import WaitAnimation from '../common/wait-animation';
import ErrorAlerts from '../alerts/error-alerts';
import ButtonWithMenu from '../common/button-with-menu';
import { Button } from '../common/buttons';
import Error from '../errors/error';
import ArchiveActionsSection from '../archive-actions/archive-actions';

// actions
import * as PromotionActions from '../../modules/promotions/details';
import * as ArchiveActions from '../../modules/promotions/archive';

// helpers
import { isArchived } from 'paragons/common';
import { transitionTo } from 'browserHistory';
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';

type Actions = {
  fetchPromotion: () => Promise,
  promotionsNew: Function,
  createPromotion: Function,
  updatePromotion: Function,
  clearSubmitErrors: Function,
  clearFetchErrors: Function,
  reset: Function,
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
  submitError: any,
  children: Element,
  fetchError: any,
  isFetching: bool,
  isSaving: boolean,
  dispatch: Function,
  archivePromotion: Function,
};

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
    this.props.actions.clearFetchErrors();
    if (this.isNew) {
      this.props.actions.promotionsNew();
    } else {
      this.props.actions.fetchPromotion(this.entityId)
        .then(({payload}) => {
          if (isArchived(payload)) transitionTo('promotions');
        });
    }
  }

  componentWillReceiveProps(nextProps) {
    const { isFetching, isSaving } = nextProps;

    if (!isFetching && !isSaving && !nextProps.fetchError) {
      const nextPromotion = nextProps.details.promotion;
      if (!nextPromotion) return;

      if (this.isNew && nextPromotion.form.id) {
        this.props.dispatch(push(`/promotions/${nextPromotion.form.id}`));
      }
      if (!this.isNew && !nextPromotion.form.id) {
        this.props.dispatch(push(`/promotions/new`));
      }
      this.setState({ promotion: nextProps.details.promotion });
    }
  }

  componentWillUnmount() {
    this.props.actions.reset();
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
        case SAVE_COMBO.NEW:
          actions.promotionsNew();
          break;
        case SAVE_COMBO.DUPLICATE:
          dispatch(push(`/promotions/new`));
          break;
        case SAVE_COMBO.CLOSE:
          dispatch(push(`/promotions`));
          break;
      }
    });
  }

  renderArchiveActions() {
    return(
      <ArchiveActionsSection type="Promotion"
                             title={this.pageTitle}
                             archive={this.archivePromotion} />
    );
  }

  @autobind
  archivePromotion() {
    this.props.archivePromotion(this.entityId).then(() => {
      transitionTo('promotions');
    });
  }

  @autobind
  handleCancel(): void {
    transitionTo('promotions');
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
            menuPosition="right"
            onPrimaryClick={this.handleSubmit}
            onSelect={this.handleSelectSaving}
            isLoading={props.isSaving}
            items={SAVE_COMBO_ITEMS}
          />
        </PageTitle>
        <SubNav promotionId={this.entityId} />
        <div styleName="promotion-details">
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
    details: state.promotions.details,
    isFetching: _.get(state.asyncActions, 'getPromotion.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getPromotion.err', null),
    isSaving: (
      _.get(state.asyncActions, 'createPromotion.inProgress', false)
      || _.get(state.asyncActions, 'updatePromotion.inProgress', false)
    ),
    submitError: (
      _.get(state.asyncActions, 'createPromotion.err') ||
      _.get(state.asyncActions, 'updatePromotion.err')
    )
  }),
  dispatch => ({
    actions: bindActionCreators(PromotionActions, dispatch),
    ...bindActionCreators(ArchiveActions, dispatch),
    dispatch,
  })
)(PromotionPage);
