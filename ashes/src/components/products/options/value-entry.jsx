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
      <TableRow id={`${valueName}-value-row`}>
        <TableCell id={`${valueName}-value-name`}>{value.name}</TableCell>
        <TableCell>{this.swatchBlock}</TableCell>
        <TableCell>{this.imageBlock}</TableCell>
        <TableCell>
          <a
            id={`${valueName}-value-edit-btn`}
            onClick={() => this.props.editValue(id, value)}
            styleName="action-icon">
            <i className="icon-edit"/>
          </a>
          <a
            id={`${valueName}-value-delete-btn`}
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
