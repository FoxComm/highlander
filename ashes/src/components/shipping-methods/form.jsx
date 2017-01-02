/* @flow weak */

import React from 'react';
import ObjectDetails from '../object-page/object-details';
const layout = require('./layout.json');

/**
 * ShippingMethodForm is a dumb component that implements the logic needed for
 * creating or updating a shipping method.
 */
export default class ShippingMethodForm extends ObjectDetails {
  layout = layout;
}
