/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import TableCell from 'components/table/cell';
import TableRow from 'components/table/row';
import SwatchDisplay from 'components/swatch/swatch-display';

// styles
import styles from './option-list.css';

// types
import type { OptionValue } from 'paragons/product';

type Props = {
  id: string|number,
  editValue: Function,
  deleteValue: Function,
  value: OptionValue,
};

class ValueEntry extends Component {
  props: Props;

  get imageBlock() {
    if (this.props.value.image) {
      return <div>{this.props.value.image}</div>;
    } else {
      return <div>No image.</div>;
    }
  }

  get swatchBlock() {
    if (this.props.value.swatch) {
      return <SwatchDisplay hexCode={this.props.value.swatch} />;
    } else {
      return <div>No hex swatch.</div>;
    }
  }

  render() {
    const { id, value } = this.props;
    const valueName = value.name.toLowerCase();

    return (
      <TableRow id={`fct-option-value-row__${valueName}`}>
        <TableCell id={`fct-option-value-name__${valueName}`}>{value.name}</TableCell>
        <TableCell>{this.swatchBlock}</TableCell>
        <TableCell>{this.imageBlock}</TableCell>
        <TableCell>
          <a
            id={`fct-value-edit-btn__${valueName}`}
            onClick={() => this.props.editValue(id, value)}
            styleName="action-icon">
            <i className="icon-edit"/>
          </a>
          <a
            id={`fct-value-delete-btn__${valueName}`}
            onClick={() => this.props.deleteValue(id)}
            styleName="action-icon">
            <i className="icon-trash"/>
          </a>
        </TableCell>
      </TableRow>
    );
  }
}

export default ValueEntry;
