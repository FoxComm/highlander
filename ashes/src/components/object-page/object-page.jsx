
// @flow weak

import _ from 'lodash';
import { connect } from 'react-redux';
import { EventEmitter } from 'events';
import { bindActionCreators } from 'redux';
import React, { Component, Element, PropTypes } from 'react';
import invariant from 'invariant';
import { push } from 'react-router-redux';
import { autobind } from 'core-decorators';
import jsen from 'jsen';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';

import styles from './object-page.css';

// components
import { PageTitle } from '../section-title';
import WaitAnimation from '../common/wait-animation';
import ErrorAlerts from '../alerts/error-alerts';
import ButtonWithMenu from '../common/button-with-menu';
import { Button } from '../common/buttons';
import Error from '../errors/error';
import ArchiveActionsSection from '../archive-actions/archive-actions';
import Prompt from '../common/prompt';

// helpers
import { isArchived } from 'paragons/common';
import { transitionTo } from 'browserHistory';
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';
import { supressTV } from 'paragons/object';

// modules
import * as SchemaActions from 'modules/object-schema';
import schemaReducer from 'modules/object-schema';

export function connectPage(namespace, actions, options = {}) {
  const capitalized = _.upperFirst(namespace);
  const plural = `${namespace}s`;
  const schemaName = options.schemaName || namespace;
  const actionNames = {
    new: `${namespace}New`, // promotionNew
    fetch: `fetch${capitalized}`, // fetchPromotion
    create: `create${capitalized}`, // createPromotion
    update: `update${capitalized}`, // updatePromotion
    archive: `archive${capitalized}`,
    sync: `sync${capitalized}`,
  };

  const requiredActions = _.values(_.omit(actionNames, 'sync'));

  function mapStateToProps(state) {
    return {
      namespace,
      plural,
      schemaName,
      capitalized,
      requiredActions,
      details: state[plural].details,
      originalObject: _.get(state, [plural, 'details', namespace]),
      isFetching: _.get(state.asyncActions, `${actionNames.fetch}.inProgress`, null),
      fetchError: _.get(state.asyncActions, `${actionNames.fetch}.err`, null),
      createError: _.get(state.asyncActions, `${actionNames.create}.err`, null),
      updateError: _.get(state.asyncActions, `${actionNames.update}.err`, null),
      archiveState: _.get(state.asyncActions, actionNames.archive, {}),
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

  function mapSchemaProps(state) {
    return {
      schema: _.get(state, schemaName),
      isSchemaFetching: _.get(state.asyncActions, 'fetchSchema.inProgress', null),
    };
  }

  function mapSchemaActions(dispatch, props) {
    return {
      actions: {
        ...props.actions,
        ...bindActionCreators(SchemaActions, dispatch),
      },
    };
  }

  return Page => {
    return _.flowRight(
      connect(mapStateToProps, mapDispatchToProps),
      makeLocalStore(addAsyncReducer(schemaReducer)),
      connect(mapSchemaProps, mapSchemaActions)
    )(Page);
  };
}

function getObjectId(object) {
  return _.get(object, 'form.id', object.id);
}

export class ObjectPage extends Component {
  state = {
    object: this.props.originalObject,
    schema: this.props.schema,
  };
  _context: {
    validationDispatcher: EventEmitter,
  };

  static childContextTypes = {
    validationDispatcher: PropTypes.object,
  };

  getChildContext() {
    return this._context || (this._context = {
      validationDispatcher: new EventEmitter()
    });
  }

  get entityIdName(): string {
    return `${this.props.namespace}Id`;
  }

  get entityId(): string {
    return this.props.params[this.entityIdName];
  }

  get contextName(): string {
    return this.props.params.context;
  }

  get isNew(): boolean {
    return this.entityId === 'new';
  }

  validateObject(object: Object): ?Array<Object> {
    const validate = jsen(this.props.schema);
    if (!validate(supressTV(object))) {
      return validate.errors;
    }
  }

  componentWillMount() {
    if (process.env.NODE_ENV != 'production') {
      // check actions is preset in props.actions
      const requiredActions = [
        'reset',
        'clearSubmitErrors',
        'clearArchiveErrors',
        'clearFetchErrors',
        ...this.props.requiredActions,
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
    this.props.actions.fetchSchema(this.props.schemaName);
    if (this.isNew) {
      this.props.actions.newEntity();
    } else {
      this.fetchEntity()
        .then(({ payload }) => {
          if (isArchived(payload)) this.transitionToList();
        });
    }
  }

  get unsaved(): boolean {
    return !_.isEqual(this.props.originalObject, this.state.object);
  }

  detailsRouteProps(): Object {
    return {};
  }

  transitionTo(id, props = {}) {
    const linkTo = _.kebabCase(this.props.namespace);
    transitionTo(`${linkTo}-details`, {
      ...this.detailsRouteProps(),
      ...props,
      [this.entityIdName]: id
    });
  }

  componentWillReceiveProps(nextProps) {
    const { isFetching, isSaving, fetchError, createError, updateError } = nextProps;

    const nextSchema = nextProps.schema;
    if (nextSchema && nextSchema != this.state.schema) {
      this.setState({
        schema: nextSchema,
      });
    }

    if (!isFetching && !isSaving && !fetchError && !createError && !updateError) {
      const nextObject = nextProps.originalObject;
      if (nextObject && nextObject != this.props.originalObject) {
        this.receiveNewObject(nextObject);
      }
    }
  }

  receiveNewObject(nextObject) {
    const nextObjectId = getObjectId(nextObject);
    const isNew = this.isNew;

    this.setState({
      object: nextObject
    }, () => {
      if (isNew && nextObjectId) {
        this.transitionTo(nextObjectId);
      }
      if (!isNew && !nextObjectId) {
        this.transitionTo('new');
      }
    });
  }

  componentWillUnmount() {
    this.props.actions.reset();
  }

  get pageTitle(): string {
    if (this.isNew) {
      return `New ${this.props.capitalized}`;
    }

    return _.get(this.props.originalObject, 'attributes.name.v', '');
  }

  @autobind
  handleUpdateObject(object) {
    this.setState({
      object,
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

  prepareObjectForValidation(object) {
    return object;
  }

  prepareObjectForSaving(object) {
    return object;
  }

  /**
   * Validates object and emit errors for handling
   * @returns {boolean} true if there is at least one errors are handled at client side
   */
  validate(): boolean {
    const errors = this.validateObject(
      supressTV(this.prepareObjectForValidation(this.state.object))
    );
    let preventSave = false;
    const event = {
      preventSave() {
        preventSave = true;
      },
      errors,
    };
    this.getChildContext().validationDispatcher.emit('errors', event);
    return !errors || !preventSave;
  }

  save() {
    let mayBeSaved = false;

    if (this.state.object) {
      const object = this.prepareObjectForSaving(this.state.object);

      if (!this.validateForm()) return;
      if (!this.validate()) return;

      if (this.isNew) {
        mayBeSaved = this.createEntity(object);
      } else {
        mayBeSaved = this.updateEntity(object);
      }
    }
    if (this.state.schema) {
      this.props.actions.saveSchema(this.props.namespace, this.state.schema);
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
          this.handleDuplicate();
          break;
        case SAVE_COMBO.CLOSE:
          this.transitionToList();
          break;
      }
    });
  }

  handleDuplicate() {
    this.transitionTo('new');
  }

  transitionToList() {
    const { dispatch, plural } = this.props;
    dispatch(push({ name: plural }));
  }

  @autobind
  archiveEntity() {
    this.props.actions.archiveEntity(this.entityId).then(() => {
      this.transitionToList();
    });
  }

  renderArchiveActions() {
    return (
      <ArchiveActionsSection
        type={this.props.capitalized}
        title={this.pageTitle}
        archive={this.archiveEntity}
        archiveState={this.props.archiveState}
        clearArchiveErrors={this.props.actions.clearArchiveErrors}
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

  @autobind
  alterSave(){
    return null;
  }

  @autobind
  titleBar() {
    return (<PageTitle title={this.pageTitle}>
        {this.renderHead()}
        <ButtonWithMenu
          title="Save"
          menuPosition="right"
          onPrimaryClick={this.handleSubmit}
          onSelect={this.handleSelectSaving}
          isLoading={this.props.isSaving}
          items={SAVE_COMBO_ITEMS}
        />
      </PageTitle>);    
  }

  childrenProps() {
    const props = this.props;
    const { object, schema } = this.state;
    const { namespace, capitalized, plural } = props;

    return {
      ...props.children.props,
      object,
      title: capitalized,
      plural,
      ref: 'form',
      isNew: this.isNew,
      schema,
      onUpdateObject: this.handleUpdateObject,
      contextName: this.contextName,
      syncEntity: props.actions.syncEntity,
      entity: { entityId: this.entityId, entityType: namespace },
    };
  }

  @autobind
  sanitizeError(error: string): string {
    return error;
  }

  renderHead() {
    return this.cancelButton;
  }

  get children() {
    return React.cloneElement(this.props.children, this.childrenProps());
  }

  get errors() {
    const { props } = this;
    return (
      <ErrorAlerts
        error={props.submitError}
        closeAction={props.actions.clearSubmitErrors}
        sanitizeError={this.sanitizeError}
      />
    );
  }

  get body() {
    const { props } = this;

    return (
      <div>
        <Prompt
          message="You have unsaved changes. Are you sure you want to leave this page?"
          when={this.unsaved}
        />
        {this.titleBar()}
        {this.subNav()}
        <div styleName="object-details">
          {this.errors}
          {this.children}
        </div>
        {!this.isNew && this.renderArchiveActions()}
        {this.alterSave()}
      </div>
    );
  }

  render(): Element {
    const props = this.props;
    const { object } = this.state;

    if (props.isFetching !== false || props.isSchemaFetching !== false) {
      return <div><WaitAnimation /></div>;
    }

    if (!object) {
      return <Error err={props.fetchError} notFound={`There is no ${props.namespace} with id ${this.entityId}`} />;
    }

    return this.body;
  }
}
