'use strict';

import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import Dropdown from '../dropdown/dropdown';

export default class TablePaginator extends React.Component {
  static propTypes = {
    setSize: PropTypes.func.isRequired
  };

  @autobind
  onPageSizeChange(value) {
    this.props.setSize(+value);
  }

  render() {
    return (
      <Dropdown
        onChange={this.onPageSizeChange}
        items={{
          '25': 'Show 25',
          '50': 'Show 50',
          '100': 'Show 100',
          'Infinity': 'Show all'
          }}
        value={'25'}
        />
    );
  }
}
