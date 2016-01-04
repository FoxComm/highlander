import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import Dropdown from '../dropdown/dropdown';
import { DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZES } from '../../modules/pagination/base';

class TablePaginator extends React.Component {
  static propTypes = {
    value: PropTypes.string,
    setState: PropTypes.func.isRequired
  };

  static defaultProps = {
    value: DEFAULT_PAGE_SIZE.toString()
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
        items={DEFAULT_PAGE_SIZES}
        value={this.props.value}
        />
    );
  }
}

export default TablePaginator;
