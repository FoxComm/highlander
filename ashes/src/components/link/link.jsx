/* @flow */
import React, { Component, Element } from 'react';
import { Link as ReactRouterLink } from 'react-router';

import { isPermitted } from 'lib/claims';
import type { Claims } from 'lib/claims';

export type LinkProps = {
  actualClaims: Claims,
  children?: Element<*>|Array<Element<*>>,
  expectedClaims: Claims,
  params: Object,
  to: string,
  activeClassName: ?string,
};

type DefaultProps = {
  actualClaims: Claims,
  expectedClaims: Claims,
  activeClassName: 'is-active',
};

export default class Link extends React.Component {
  props: LinkProps;

  static defaultProps: DefaultProps = {
    actualClaims: {},
    expectedClaims: {},
  };

  render() {
    let { to, params, children, actualClaims, expectedClaims, activeClassName, ...otherProps } = this.props;
    let location = {
      name: to,
      params,
    };

    if (!isPermitted(expectedClaims, actualClaims)) {
      return <div />;
    }

    return (
      <ReactRouterLink activeClassName={activeClassName} {...otherProps} to={location}>
        {children}
      </ReactRouterLink>
    );
  }
}
