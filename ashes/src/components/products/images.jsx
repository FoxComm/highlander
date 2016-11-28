/* @flow weak */

import React from 'react';
import ImagesPage, { connectImages } from '../object-page/object-images';

import { actions } from 'modules/products/images';

const ProductImages = class extends ImagesPage {};

export default connectImages('product', actions)(ProductImages);
