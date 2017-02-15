/* @flow */

// libs
import React from 'react';
import type { HTMLElement } from 'types';

// types
import { SphexTracker, sphexTrackerUrl } from 'modules/tracking';

// styles
import styles from './tracking-pixel.css';

const TrackingPixel = (props: SphexTracker): HTMLElement => (
  <img styleName="tracking-style" alt="" src={sphexTrackerUrl(props)} />
);

export default TrackingPixel;
