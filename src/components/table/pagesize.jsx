'use strict';

import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import Dropdown from '../dropdown/dropdown';

class TablePaginator extends React.Component {
  static propTypes = {
    value: PropTypes.string,
    setState: PropTypes.func.isRequired
  };

  static defaultProps = {
    value: '50'
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
        value={this.props.value}
        />
    );
  }
}

export default TablePaginator;
