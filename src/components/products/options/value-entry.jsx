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

// styles
import styles from './option-list.css';

// types
import type { VariantValue } from 'paragons/product';

type Props = {
  id: string|number,
  editValue: Function,
  deleteValue: Function,
  value: VariantValue,
};

class ValueEntry extends Component {
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
    const { id, value } = this.props;

    return (
      <TableRow>
        <TableCell>{value.name}</TableCell>
        <TableCell>{this.swatchBlock}</TableCell>
        <TableCell>{this.imageBlock}</TableCell>
        <TableCell>
          <a onClick={() => this.props.editValue(id, value)} styleName="action-icon"><i className="icon-edit"/></a>
          <a onClick={() => this.props.deleteValue(id)} styleName="action-icon"><i className="icon-trash"/></a>
        </TableCell>
      </TableRow>
    );
  }
}

export default ValueEntry;
