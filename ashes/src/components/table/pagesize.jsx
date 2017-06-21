import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import { TextDropdown } from 'components/core/dropdown';
import { DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZES } from '../../modules/pagination/base';

// styles
import s from './pagesize.css';

class TablePaginator extends React.Component {
  static propTypes = {
    value: PropTypes.number,
    setState: PropTypes.func.isRequired,
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
      <TextDropdown
        className={s.dropdown}
        onChange={this.onPageSizeChange}
        items={DEFAULT_PAGE_SIZES}
        value={String(this.props.value)}
      />
    );
  }
}

export default TablePaginator;
