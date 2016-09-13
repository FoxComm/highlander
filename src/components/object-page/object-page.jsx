// @flow weak

import _ from 'lodash';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import React, { Component, Element } from 'react';
import invariant from 'invariant';
import { push } from 'react-router-redux';
import { autobind } from 'core-decorators';

import styles from './object-page.css';

// components
import { PageTitle } from '../section-title';
import WaitAnimation from '../common/wait-animation';
import ErrorAlerts from '../alerts/error-alerts';
import ButtonWithMenu from '../common/button-with-menu';
import { Button } from '../common/buttons';
import Error from '../errors/error';
import ArchiveActionsSection from '../archive-actions/archive-actions';

// helpers
import { isArchived } from 'paragons/common';
import { transitionTo } from 'browserHistory';
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';

export function connectPage(namespace, actions) {
  const capitalized = _.upperFirst(namespace);
  const plural = `${namespace}s`;
  const actionNames = {
    new: `${namespace}New`, // promotionsNew
    fetch: `fetch${capitalized}`, // fetchPromotion
    create: `create${capitalized}`, // createPromotion
    update: `update${capitalized}`, // updatePromotion
    archive: `archive${capitalized}`,
  };

  function mapStateToProps(state) {
    return {
      namespace,
      plural,
      capitalized,
      actionNames,
      details: state[plural].details,
      isFetching: _.get(state.asyncActions, `${actionNames.fetch}.inProgress`, null),
      fetchError: _.get(state.asyncActions, `${actionNames.fetch}.err`, null),
      isSaving: (
        _.get(state.asyncActions, `${actionNames.create}.inProgress`, false)
        || _.get(state.asyncActions, `${actionNames.update}.inProgress`, false)
      ),
      submitError: (
        _.get(state.asyncActions, `${actionNames.create}.err`) ||
        _.get(state.asyncActions, `${actionNames.update}.err`)
      )
    };
  }

  function generalizeActions(actions) {
    const result = {
      ...actions,
    };

    _.each(actionNames, (name, key) => {
      result[`${key}Entity`] = actions[name];
    });

    return result;
  }

  function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators(generalizeActions(actions), dispatch),
      dispatch,
    };
  }

  return Page => {
    return connect(mapStateToProps, mapDispatchToProps)(Page);
  };
}

function getObjectId(object) {
  return _.get(object, 'form.id', object.id);
}

export class ObjectPage extends Component {
  state = {
    [this.props.namespace]: this.props.details[this.props.namespace],
  };

  get entityIdName(): string {
    return `${this.props.namespace}Id`;
  }

  get entityId(): string {
    return this.props.params[this.entityIdName];
  }

  get isNew(): boolean {
    return this.entityId === 'new';
  }

  componentWillMount() {
    if (process.env.NODE_ENV != 'production') {
      // check actions is preset in props.actions
      const requiredActions = [
        'reset',
        'clearSubmitErrors',
        'clearFetchErrors',
        ..._.values(this.props.actionNames),
      ];
      _.each(requiredActions, name => {
        invariant(
          typeof this.props.actions[name] != 'undefined',
          `${this.constructor.name} should contain ${name} action in props.actions namespace`
        );
      });
    }
  }

  fetchEntity(): Promise {
    return this.props.actions.fetchEntity(this.entityId);
  }

  componentDidMount() {
    this.props.actions.clearFetchErrors();
    if (this.isNew) {
      this.props.actions.newEntity();
    } else {
      this.fetchEntity()
        .then(({payload}) => {
          if (isArchived(payload)) this.transitionToList();
        });
    }
  }

  detailsRouteProps(): Object {
    return {};
  }

  transitionTo(id, props={}) {
    transitionTo(`${this.props.namespace}-details`, {
      ...this.detailsRouteProps(),
      ...props,
      [this.entityIdName]: id
    });
  }

  componentWillReceiveProps(nextProps) {
    const { isFetching, isSaving } = nextProps;
    const { namespace } = this.props;

    if (!isFetching && !isSaving && !nextProps.fetchError) {
      const nextEntity = nextProps.details[namespace];
      if (!nextEntity) return;

      const nextEntityId = getObjectId(nextEntity);

      if (this.isNew && nextEntityId) {
        this.transitionTo(nextEntityId);
      }
      if (!this.isNew && !nextEntityId) {
        this.transitionTo('new');
      }
      this.setState({
        entity: nextProps.details[namespace]
      });
    }
  }

  componentWillUnmount() {
    this.props.actions.reset();
  }

  get entity() {
    return this.props.details[this.props.namespace];
  }

  get pageTitle(): string {
    if (this.isNew) {
      return `New ${this.props.capitalized}`;
    }

    return _.get(this.entity, 'form.attributes.name', '');
  }

  @autobind
  handleUpdateEntity(entity) {
    this.setState({
      entity,
    });
  }

  validateForm(): boolean {
    const { form } = this.refs;

    let formValid = true;

    if (form && form.checkValidity) {
      if (!form.checkValidity()) formValid = false;
    }

    return formValid;
  }

  createEntity(entity) {
    return this.props.actions.createEntity(entity);
  }

  updateEntity(entity) {
    return this.props.actions.updateEntity(entity);
  }

  save() {
    let mayBeSaved = false;

    if (this.state.entity) {
      const entity = this.state.entity;

      if (!this.validateForm()) return;

      if (this.isNew) {
        mayBeSaved = this.createEntity(entity);
      } else {
        mayBeSaved = this.updateEntity(entity);
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
    const { actions } = this.props;
    const mayBeSaved = this.save();
    if (!mayBeSaved) return;

    mayBeSaved.then(() => {
      switch (value) {
        case SAVE_COMBO.NEW:
          actions.newEntity();
          break;
        case SAVE_COMBO.DUPLICATE:
          this.transitionTo('new');
          break;
        case SAVE_COMBO.CLOSE:
          this.transitionToList();
          break;
      }
    });
  }

  transitionToList() {
    const { dispatch, plural } = this.props;
    dispatch(push(`/${plural}`));
  }

  @autobind
  archiveEntity() {
    this.props.actions.archiveEntity(this.entityId).then(() => {
      this.transitionToList();
    });
  }

  renderArchiveActions() {
    return(
      <ArchiveActionsSection
        type={this.props.capitalized}
        title={this.pageTitle}
        archive={this.archiveEntity}
      />
    );
  }

  @autobind
  handleCancel(): void {
    this.transitionToList();
  }

  get cancelButton(): ?Element {
    if (this.isNew) {
      return (
        <Button
          type="button"
          onClick={this.handleCancel}
          styleName="cancel-button">
          Cancel
        </Button>
      );
    }
  }

  subNav(): ?Element {
    return null;
  }

  childrenProps() {
    const props = this.props;
    const { entity } = this.state;
    const { namespace, capitalized } = props;

    return {
      ...props.children.props,
      [namespace]: entity,
      ref: 'form',
      [`onUpdate${capitalized}`]: this.handleUpdateEntity,
      entity: { entityId: this.entityId, entityType: namespace },
    };
  }

  @autobind
  sanitizeError(error: string): string {
    return error;
  }

  get preventSave(): boolean {
    return false;
  }

  renderHead() {
    return this.cancelButton;
  }

  render(): Element {
    const props = this.props;
    const { entity } = this.state;
    const { actions, namespace } = props;

    if (props.isFetching !== false && !entity) {
      return <div><WaitAnimation /></div>;
    }

    if (!entity) {
      return <Error err={props.fetchError} notFound={`There is no ${namespace} with id ${this.entityId}`} />;
    }

    const children = React.cloneElement(props.children, this.childrenProps());

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          {this.renderHead()}
          <ButtonWithMenu
            title="Save"
            menuPosition="right"
            onPrimaryClick={this.handleSubmit}
            onSelect={this.handleSelectSaving}
            isLoading={props.isSaving}
            items={SAVE_COMBO_ITEMS}
            buttonDisabled={this.preventSave}
          />
        </PageTitle>
        {this.subNav()}
        <div styleName="object-details">
          <ErrorAlerts
            error={this.props.submitError}
            closeAction={actions.clearSubmitErrors}
            sanitizeError={this.sanitizeError}
          />
          {children}
        </div>
        {!this.isNew && this.renderArchiveActions()}
      </div>
    );
  }
}
