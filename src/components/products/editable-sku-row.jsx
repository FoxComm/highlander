/**
 * @flow
 */

import React, { Component, Element } from 'react'

import type { IlluminatedSku } from '../../paragons/product';

type Props = {
  sku: IlluminatedSku,
};

export default class EditableSkuRow extends Component<void, Props, void> {
  render(): Element {
    return <tr>{this.props.sku.code}</tr>;
  }
}
