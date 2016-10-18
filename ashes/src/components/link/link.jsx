/* @flow */
import React, { Component, Element } from 'react';
import { Link as ReactRouterLink } from 'react-router';

import { isPermitted } from 'lib/claims';
import type { Claims } from 'lib/claims';

type Props = {
  actualClaims: Claims,
  children: Element|Array<Element>,
  expectedClaims: Claims,
  params: Object,
  to: string,
};

type DefaultProps = {
  actualClaims: Claims,
  expectedClaims: Claims,
};

export default class Link extends React.Component {
  props: Props;

  static defaultProps: DefaultProps = {
    actualClaims: {},
    expectedClaims: {},
  };

  render(): Element {
    let {to, params, children, actualClaims, expectedClaims, ...otherProps} = this.props;
    let location = {
      name: to,
      params,
    };

    if (!isPermitted(expectedClaims, actualClaims)) {
      return <div></div>;
    }

    return (
      <ReactRouterLink activeClassName="is-active" {...otherProps} to={location} >
        {children}
      </ReactRouterLink>
    );
  }
}
