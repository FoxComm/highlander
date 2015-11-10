import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { LeftButton, RightButton } from '../common/buttons';

class TablePaginator extends React.Component {
  static propTypes = {
    total: PropTypes.number.isRequired,
    from: PropTypes.number.isRequired,
    size: PropTypes.number.isRequired,
    setState: PropTypes.func.isRequired
  };

  @autobind
  onPrevPageClick() {
    this.props.setState({
      from: Math.max(0, Math.min(this.props.total - 1, this.props.from - this.props.size))
    });
  }

  @autobind
  onNextPageClick() {
    this.props.setState({
      from: Math.max(0, Math.min(this.props.total - 1, this.props.from + this.props.size))
    });
  }

  render() {
    const total = this.props.total;
    const from = this.props.from + 1;
    const end = Math.min(total, this.props.from + this.props.size);
    return (
      <div className="fc-table-paginator">
        <span>
          {from}&thinsp;-&thinsp;{end} of {total}
        </span>
        &nbsp;
        <LeftButton onClick={this.onPrevPageClick}/>
        &nbsp;
        <RightButton onClick={this.onNextPageClick}/>
      </div>
    );
  }
}

export default TablePaginator;
