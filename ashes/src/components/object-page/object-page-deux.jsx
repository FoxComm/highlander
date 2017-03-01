// @flow

import React, { Component, Element } from 'react';
import { IndexLink, Link } from 'components/link';
import { PageTitle } from 'components/section-title';
import Error from 'components/errors/error';
import LocalNav from 'components/local-nav/local-nav';
import SaveCancel from 'components/common/save-cancel';
import WaitAnimation from 'components/common/wait-animation';
import { autobind } from 'core-decorators';

// helpers
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';
import { transitionTo } from 'browserHistory';

class ObjectPageDeux extends Component {
  // TODO: replace *
  props: ObjectProps<*, *>;

  componentDidMount() {
    const { context, identifier } = this.props;
    this.props.actions.fetch(identifier, context);
  }

  get isNew(): boolean {
    const { identifier, object } = this.props;
    return identifier.toLowerCase() === 'new' || !object;
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
        >
          {settings.title}
        </LinkComponent>
      );
    });

    return <LocalNav>{links}</LocalNav>;
  }


  get pageTitle(): string {
    if (this.isNew) {
      const { objectType } = this.props;
      return `New ${objectType}`;
    }

    const { originalObject } = this.props;
    const { getTitle } = this.props.actions;
    return getTitle(originalObject);
  }

  @autobind
  createNewEntity(actions, context, objectType) {
    const params = { [`${objectType}Id`]: 'new', context: context };
    const createEntity = actions[`${objectType}New`];
    createEntity !== undefined
      ? createEntity() && transitionTo(`${objectType}-details`, params)
      : () => {};
  }

  @autobind
  duplicateEntity(actions, context, objectType) {
    const params = { [`${objectType}Id`]: 'new', context: context };
    const duplicate = actions[`${objectType}Duplicate`];
    duplicate !== undefined
      ? duplicate() && transitionTo(`${objectType}-details`, params)
      : () => {};
  }

  @autobind
  handleSelectSaving(value) {
    const { actions, context, objectType} = this.props;
    this.save().then(() => {
      switch (value) {
        case SAVE_COMBO.NEW:
          this.createNewEntity(actions, context, objectType);
          break;
        case SAVE_COMBO.DUPLICATE:
          this.duplicateEntity(actions, context, objectType);
          break;
        case SAVE_COMBO.CLOSE:
          actions.cancel();
          break;
      }
    })
  }

  @autobind
  save() {
    const { context, object } = this.props;
    return this.isNew
      ? this.props.actions.create(object, context)
      : this.props.actions.update(object, context);
  }

  renderButtonCluster() {
    const { isFetching } = this.props;

    return (
      <SaveCancel
        isLoading={isFetching}
        onCancel={this.props.actions.cancel}
        saveItems={SAVE_COMBO_ITEMS}
        onSave={this.save}
        onSaveSelect={this.handleSelectSaving}
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
          {this.renderButtonCluster()}
        </PageTitle>
        {this.localNav}
        {children}
      </div>
    );
  }
}

export default ObjectPageDeux;
