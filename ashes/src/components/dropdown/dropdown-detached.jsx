/* @flow */

import React, { Component, Element } from 'react';

import Dropdown from './dropdown';
import BodyPortal from '../body-portal/body-portal';

import type { Props } from './generic-dropdown';

export default (props: Props): HTMLElement => (
  <Dropdown
    {...props}
    wrapMenu={(menu => <BodyPortal>{menu}</BodyPortal>)}
  />
);
