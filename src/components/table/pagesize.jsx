'use strict';

import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import Dropdown from '../dropdown/dropdown';

class TablePaginator extends React.Component {
  static propTypes = {
    setState: PropTypes.func.isRequired
  };

  @autobind
  onPageSizeChange(value) {
    this.props.setState({
      size: +value
    });
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

export default TablePaginator;
