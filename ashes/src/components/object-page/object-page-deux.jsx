// @flow

import { pluralize } from 'fleck';
import { capitalize, noop } from 'lodash';
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
  props: ObjectProps<*, *>;

  componentDidMount() {
    const { context, identifier, actions } = this.props;

    actions.fetch(identifier, context);
  }

  get isNew(): boolean {
    const { identifier, object } = this.props;

    return identifier.toString().toLowerCase() === 'new' || !object;
  }

  get localNav() {
    if (this.isNew) {
      return null;
    }

    const links = this.props.navLinks.map((settings, idx) => {
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
    const { identifier, actions } = this.props;

    actions.transition(identifier);
  }

  @autobind
  save() {
    const { context, object, actions } = this.props;

    const saveFn = this.isNew ? actions.create : actions.update;

    return saveFn(object, context);
  }

  @autobind
  archive() {
    const { identifier, objectType, actions } = this.props;

    const plural = pluralize(objectType);

    actions.archive(identifier).then(transitionToLazy(plural));
  }

  get headerControls() {
    const { isFetching } = this.props;

    return (
      <SaveCancel
        isLoading={isFetching}
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

  render() {
    const { children, identifier, isFetching, fetchError, object, objectType } = this.props;
    if (isFetching) {
      return <div><WaitAnimation /></div>;
    }

    if (!object) {
      return <Error err={fetchError} notFound={`There is no ${objectType} with id ${identifier}.`} />;
    }

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          {this.headerControls}
        </PageTitle>
        {this.localNav}
        {children}
        {this.footerControls}
      </div>
    );
  }
}

export default ObjectPageDeux;
