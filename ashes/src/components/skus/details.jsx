/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import ObjectDetails from '../object-page/object-details';
const layout = require('./layout.json');

export default class SkuDetails extends ObjectDetails {
  layout = layout;
}
