
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classnames from 'classnames';

// components
import { LeftButton, RightButton } from '../common/buttons';
import { Dropdown } from '../dropdown';

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

  @autobind
  currentPageSelector(currentPage, pageCount) {
    console.log(pageCount);
    const pageSelectorClass = classnames({'_disabled': pageCount <= 1});
    const pages = _.range(1, pageCount).reduce((acc, item) => acc[item.toString()] = item, {});
    return (
      <Dropdown className={pageSelectorClass} items={pages} editable={pageCount > 1} value={currentPage.toString()}/>
    );
  }

  render() {
    const currentPage = Math.ceil((this.props.total - this.props.from) / this.props.size);
    const pageCount = Math.ceil(this.props.total / this.props.size);
    const leftButtonClass = classnames({'_hidden': currentPage <= 1});
    const rightButtonClass = classnames({'_hidden': currentPage >= pageCount});
    return (
      <div className="fc-table-paginator">
        <LeftButton className={leftButtonClass} onClick={this.onPrevPageClick}/>
        <div className="fc-table-paginator__current-page">
          {this.currentPageSelector(currentPage, pageCount)}
        </div>
        <div className="fc-table-paginator__separator">of</div>
        <div className="fc-table-paginator__total-pages">
          {pageCount}
        </div>
        <RightButton className={rightButtonClass} onClick={this.onNextPageClick}/>
      </div>
    );
  }
}

export default TablePaginator;
