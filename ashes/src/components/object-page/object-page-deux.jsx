// @flow

import React, { Component, Element } from 'react';
import { IndexLink, Link } from 'components/link';
import { PageTitle } from 'components/section-title';
import Error from 'components/errors/error';
import LocalNav from 'components/local-nav/local-nav';
import SaveCancelWithMenu from 'components/common/save-cancel-with-menu';
import WaitAnimation from 'components/common/wait-animation';

// helpers
import { SAVE_COMBO, SAVE_COMBO_ITEMS } from 'paragons/common';

export type ObjectActions<T> = {
  reset: () => void,
  fetch: (id: string, context?: string) => void,
  create: (object: T, context?: string) => void,
  update: (object: T, context?: string) => void,
  archive: (object: T, context?: string) => void,
  cancel: () => void,

  getTitle: (object: T) => string,
};

export type ObjectProps<T, U> = {
  actions: ObjectActions<T>,
  children?: Element|Array<Element>,
  context?: string,
  identifier: string,
  isFetching: boolean,
  fetchError: ?Object,
  navLinks: NavLinks<U>,
  object: ?T,
  objectType: string,
  originalObject: ?T,
};

class ObjectPageDeux extends Component {
  props: ObjectProps;

  componentDidMount() {
    const { context, identifier } = this.props;
    this.props.actions.fetch(identifier, context);
  }

  get isNew(): boolean {
    const { identifier, object } = this.props;
    return identifier.toLowerCase() === 'new' || !object;
  }

  get localNav(): ?Element {
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

  renderButtonCluster(): Element {
    const { isFetching, context, object } = this.props;
    const save = () => this.isNew
      ? this.props.actions.create(object, context)
      : this.props.actions.update(object, context);

    return (
      <SaveCancelWithMenu
        isLoading={isFetching}
        onCancel={this.props.actions.cancel}
        primaryItems={SAVE_COMBO_ITEMS}
        onPrimaryClick={save}
        onPrimarySelect={() => {}}
      />
    );
  }

  render(): Element {
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
