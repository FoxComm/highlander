/* @flow */

import React, { Component, Element } from 'react';

type Props = {
  children?: string|Element<*>|Array<Element<*>>,
};

export default class CatalogsListWrapper extends Component {
  render() {
    const { children } = this.props;
    return (
      <div>
        {children}
      </div>
    );
  }
}
