// @flow

import { get, capitalize, noop, pick } from 'lodash';
import React, { Component, Element } from 'react';
import { IndexLink, Link } from 'components/link';
import { PageTitle } from 'components/section-title';
import Error from 'components/errors/error';
import LocalNav from 'components/local-nav/local-nav';
import SaveCancel from 'components/common/save-cancel';
import WaitAnimation from 'components/common/wait-animation';
import { autobind } from 'core-decorators';

// components
import ArchiveActionsSection from 'components/archive-actions/archive-actions';

// helpers
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';
import { transitionToLazy } from 'browserHistory';

class ObjectPageDeux extends Component {
  // TODO: replace *
  props: ObjectPageProps<*, *>;

  static defaultProps = {
    identifierFieldName: 'id',
  };

  componentDidMount() {
    const { context, identifier, actions } = this.props;

    actions.fetch(identifier, context);
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
          key={settings.key}
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
    this.save().then(this.transitionToObject);
  }

  @autobind
  transitionToObject() {
    const { object, identifierFieldName, actions } = this.props;

    actions.transition(get(object, identifierFieldName));
  }

  @autobind
  save() {
    const { context, object, actions } = this.props;

    const saveFn = this.isNew ? actions.create : actions.update;

    return saveFn(object, context);
  }

  @autobind
  archive() {
    const { identifier, actions } = this.props;

    actions.archive(identifier).then(actions.close);
  }

  get headerControls() {
    return (
      <SaveCancel
        isLoading={this.props.saveState.inProgress}
        onCancel={this.props.actions.close}
        saveItems={SAVE_COMBO_ITEMS}
        onSave={this.handleSaveButton}
        onSaveSelect={this.handleSelectSaving}
      />
    );
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
    const { layout, schema, objectType, object, onUpdateObject } = this.props;

    return React.cloneElement(React.Children.only(this.props.children), {
      layout,
      schema,
      objectType,
      object,
      onUpdateObject,
    });
  }

  render() {
    const { schema, objectType, object, identifier, fetchState } = this.props;

    if (fetchState.inProgress) {
      return <div><WaitAnimation /></div>;
    }

    if (!object || !schema) {
      return <Error err={fetchState.err} notFound={`There is no ${objectType} with id ${identifier}.`} />;
    }

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          {this.headerControls}
        </PageTitle>
        {this.localNav}
        {this.children}
        {this.footerControls}
      </div>
    );
  }
}

export default ObjectPageDeux;
