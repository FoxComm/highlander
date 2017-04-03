// @flow

import { get, capitalize, noop, pick } from 'lodash';
import { autobind } from 'core-decorators';
import EventEmitter from 'events';
import jsen from 'jsen';
import React, { Component, Element, PropTypes } from 'react';

// components
import { IndexLink, Link } from 'components/link';
import { PageTitle } from 'components/section-title';
import Error from 'components/errors/error';
import LocalNav from 'components/local-nav/local-nav';
import WaitAnimation from 'components/common/wait-animation';
import ArchiveActionsSection from 'components/archive-actions/archive-actions';
import ButtonWithMenu from '../common/button-with-menu';


// helpers
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';
import { supressTV } from 'paragons/object';

import styles from './object-page.css';

class ObjectPageDeux extends Component {
  // TODO: replace *
  props: ObjectPageProps<*, *>;

  static defaultProps = {
    identifierFieldName: 'id',
    headerControls: [],
  };

  _context: {
    validationDispatcher: EventEmitter,
  };

  static childContextTypes = {
    validationDispatcher: PropTypes.object,
  };

  componentDidMount() {
    const { context, identifier, actions } = this.props;

    actions.fetch(identifier, context);
  }

  componentWillReceiveProps(nextProps: ObjectPageProps<*, *>) {
    const { actions, identifier, context } = nextProps;

    if (this.props.identifier !== identifier && nextProps.identifier !== 'new') {
      actions.fetch(identifier, context);
    }
  }

  getChildContext() {
    if (!this._context) {
      const emitter = new EventEmitter();

      emitter.setMaxListeners(20);

      this._context = {
        validationDispatcher: emitter
      };
    }

    return this._context;
  }

  get isNew(): boolean {
    const { identifier, object } = this.props;

    return identifier.toString().toLowerCase() === 'new' || !object;
  }

  get localNav() {
    const navLinks = this.isNew ? this.props.navLinks.slice(0, 1) : this.props.navLinks;

    const links = navLinks.map((settings, idx) => {
      const LinkComponent = idx === 0 ? IndexLink : Link;

      return (
        <LinkComponent
          to={settings.to}
          params={settings.params}
          key={settings.to}
          children={settings.title}
        />
      );
    });

    return <LocalNav>{links}</LocalNav>;
  }

  get pageTitle(): string {
    const { objectType, originalObject, actions } = this.props;

    if (this.isNew) {
      return `New ${objectType}`;
    }

    return actions.getTitle(originalObject);
  }

  @autobind
  createNewEntity() {
    this.props.actions.reset();
    this.props.actions.transition('new');
  }

  @autobind
  duplicateEntity() {
    this.props.actions.duplicate();
    this.props.actions.transition('new');
  }

  @autobind
  handleSelectSaving(value: string) {
    const { actions } = this.props;
    if (!this.save()) { return }

    this.save().then(() => {
      switch (value) {
        case SAVE_COMBO.NEW:
          this.createNewEntity();
          break;
        case SAVE_COMBO.DUPLICATE:
          this.duplicateEntity();
          break;
        case SAVE_COMBO.CLOSE:
          actions.close();
          break;
      }
    });
  }

  @autobind
  handleSaveButton() {
    if (!this.save()) { return }
    this.save().then(this.transitionToObject);
  }

  @autobind
  transitionToObject() {
    const { object, identifierFieldName, actions } = this.props;

    actions.transition(get(object, identifierFieldName));
  }

  prepareObjectForValidation(object) {
    return object;
  }

  prepareObjectForSaving(object) {
    return object;
  }

  validateForm(): boolean {
    const { form } = this.refs;

    let formValid = true;

    if (form && form.checkValidity) {
      if (!form.checkValidity())  formValid = false;
    };

    return formValid;
  }

  validateObject(object: Object): ?Array<Object> {
    const validate = jsen(this.props.schema);
    if (!validate(supressTV(object))) {
      return validate.errors;
    }
  }

  validate(): boolean {
    const errors = this.validateObject(
      supressTV(this.prepareObjectForValidation(this.props.object))
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

  @autobind
  save() {
    const { context, actions } = this.props;
    const object = this.prepareObjectForSaving(this.props.object);

    if (!this.validateForm()) return;
    if (!this.validate()) return;

    const saveFn = this.isNew ? actions.create : actions.update;

    return saveFn(object, context);
  }

  @autobind
  archive() {
    const { identifier, actions } = this.props;

    actions.archive(identifier).then(actions.close);
  }

  get headerControls(): Array<any> {
    return [
      ...this.props.headerControls,
      <ButtonWithMenu
        title="Save"
        menuPosition="right"
        onPrimaryClick={this.handleSaveButton}
        onSelect={this.handleSelectSaving}
        isLoading={this.props.saveState.inProgress}
        items={SAVE_COMBO_ITEMS}
        key="save-btn"
      />
    ];
  }

  get footerControls() {
    if (this.isNew) {
      return null;
    }

    const { object, objectType, actions, archiveState } = this.props;

    return (
      <ArchiveActionsSection
        type={capitalize(objectType)}
        title={actions.getTitle(object)}
        archive={this.archive}
        archiveState={archiveState}
        clearArchiveErrors={actions.clearArchiveErrors}
      />
    );
  }

  get children(): Element<*> {
    const { layout, schema, object, objectType, internalObjectType, onUpdateObject } = this.props;
    const ref = 'form';

    return React.cloneElement(this.props.children, {
      ref,
      layout,
      schema,
      object,
      objectType,
      internalObjectType,
      onUpdateObject,
    });
  }

  render() {
    const { object, objectType, identifier, fetchState } = this.props;

    if (fetchState.err) {
      return <Error err={fetchState.err} notFound={`There is no ${objectType} with id ${identifier}.`} />;
    }

    if (!object || fetchState.inProgress) {
      return <WaitAnimation className={styles.waiting} />;
    }

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          {this.headerControls}
        </PageTitle>
        {this.localNav}
        <div styleName="object-details">
          {this.children}
        </div>
        {this.footerControls}
      </div>
    );
  }
}

export default ObjectPageDeux;
