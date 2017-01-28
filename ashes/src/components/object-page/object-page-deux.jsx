// @flow

import React, { Component, Element } from 'react';
import { PageTitle } from 'components/section-title';
import Error from 'components/errors/error';
import WaitAnimation from 'components/common/wait-animation';

export type ObjectActions<T> = {
  reset: () => void,
  fetch: (id: string, context?: string) => void,
  create: (object: T, context?: string) => void,
  update: (object: T, context?: string) => void,
  archive: (object: T, context?: string) => void,

  getTitle: (object: T) => string,
};

export type ObjectProps<T> = {
  actions: ObjectActions<T>,
  context?: string,
  identifier: string,
  isFetching: boolean,
  fetchError: ?Object,
  object: ?T,
  objectType: string,
};

class ObjectPageDeux extends Component {
  props: ObjectProps;

  componentDidMount() {
    const { context, identifier } = this.props;
    this.props.actions.fetch(identifier, context);
  }

  get pageTitle(): string {
    const { identifier, object } = this.props;
    if (identifier.toLowerCase() === 'new' || !object) {
      const { objectName } = this.props;
      return `New ${objectName}`;
    }

    const { getTitle } = this.props.actions;
    return getTitle(object);
  }

  render(): Element {
    const { identifier, isFetching, fetchError, object, objectType } = this.props;
    if (isFetching) {
      return <div><WaitAnimation /></div>;
    }

    if (!object) {
      return <Error err={fetchError} notFound={`There is no ${objectType} with id ${identifier}.`} />;
    }

    return (
      <div>
        <PageTitle title={this.pageTitle}>
        </PageTitle>
      </div>
    );
  }
}

export default ObjectPageDeux;
