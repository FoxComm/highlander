/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';

// components
import TableCell from 'components/table/cell';
import TableRow from 'components/table/row';
import SwatchDisplay from 'components/swatch/swatch-display';

// types
import type { VariantValue } from 'paragons/product';

type Props = {
  name: string,
  value: VariantValue,
};

export default class VariantValueEntry extends Component<void, Props, void> {
  props: Props;

  get imageBlock(): Element {
    if (this.props.value.image) {
      return <div>{this.props.value.image}</div>;
    } else {
      return <div>No image.</div>;
    }
  }

  get swatchBlock(): Element {
    if (this.props.value.swatch) {
      return <SwatchDisplay hexCode={this.props.value.swatch} />;
    } else {
      return <div>No hex swatch.</div>;
    }
  }

  render(): Element {
    const { name, value } = this.props;
    return (
      <TableRow>
        <TableCell>{_.capitalize(name)}</TableCell>
        <TableCell>{value.id}</TableCell>
        <TableCell>{this.swatchBlock}</TableCell>
        <TableCell>{this.imageBlock}</TableCell>
      </TableRow>
    );
  }
}
