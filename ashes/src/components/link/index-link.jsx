/* @flow */
import React, { Component, Element } from 'react';
import Link from './link';

import { isPermitted } from 'lib/claims';
import type { Claims } from 'lib/claims';

type Props = {
  children: Element|Array<Element>,
  actualClaims: Claims,
  expectedClaims: Claims,
};

type DefaultProps = {
  actualClaims: Claims,
  expectedClaims: Claims,
};

export default class IndexLink extends Component {
  props: Props;

  static defaultProps: DefaultProps = {
    actualClaims: {},
    expectedClaims: {},
  };

  render(): Element {
    const { children, actualClaims, expectedClaims } = this.props;
    if (isPermitted(expectedClaims, actualClaims)) {
      return <Link {...this.props} onlyActiveOnIndex={true}>{children}</Link>;
    }

    return <div></div>;
  }
}
