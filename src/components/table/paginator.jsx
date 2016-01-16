
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

  constructor(props, ...args) {
    super(props, ...args);
    const page = this.props.size !== 0 ? Math.ceil(this.props.from / this.props.size + 1) : 1;
    this.state = {
      optionsDisplayed: false,
      pageToDisplay: page
    };
  }

  componentWillReceiveProps(newProps) {
    const page = this.props.size !== 0 ? Math.ceil(this.props.from / this.props.size + 1) : 1;
    this.setState({pageToDisplay: page});
  }

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
  openOptions() {
    this.setState({optionsDisplayed: true});
  }

  @autobind
  closeOptions(value) {
    const from = Math.max(0, Math.min(this.props.total - 1, this.props.size * (value - 1)));
    this.setState({optionsDisplayed: false}, () => this.props.setState({from: from}));
  }

  @autobind
  closeOptionsWithoutChange() {
    this.setState({optionsDisplayed: false});
  }

  @autobind
  onValueSelect(value) {
    const from = Math.max(0, Math.min(this.props.total - 1, this.props.size * (value - 1)));
    this.setState({optionsDisplayed: false}, () => this.props.setState({from: from}));
  }

  @autobind
  onPageChange(value) {
    this.setState({pageToDisplay: value});
  }

  @autobind
  currentPageSelector(pageCount) {
    const pageSelectorClass = classnames('currentPage', {'_disabled': pageCount <= 1});
    const disabledOption = pageCount <= 1 ? {disabled: true}: {};
    const pages = _.range(1, pageCount + 1).map((item) => {
      return (
        <li className="fc-table-paginator__selector-option"
            value={item}
            key={item}
            onClick={() => this.onValueSelect(item)} >
          {item}
        </li>
      );
    });
    const inputClass = classnames('fc-table-paginator__current-page-field', '_no-counters', {
      '_disabled': pageCount <= 1
    });
    const optionsClass = classnames('fc-table-paginator__current-page-selector', {
      '_shown': this.state.optionsDisplayed
    });
    const overlayClass = classnames('fc-table-paginator__current-page-selector-overlay', {
      '_shown': this.state.optionsDisplayed
    });
    return (
      <div className="fc-form-field">
        <input className={inputClass}
               name="currentPage"
               type="number"
               value={this.state.pageToDisplay}
               onFocus={this.openOptions}
               onChange={({target}) => this.onPageChange(target.value)}
               {...disabledOption} />
        <div className={optionsClass} >
          <ul>
            {pages}
          </ul>
        </div>
        <div className={overlayClass} onClick={this.closeOptionsWithoutChange}></div>
      </div>
    );
  }

  render() {
    const currentPage = this.props.size !== 0 ? Math.ceil(this.props.from / this.props.size + 1) : 1;
    const pageCount = this.props.size !== 0 ? Math.ceil(this.props.total / this.props.size) : 1;
    const leftButtonClass = classnames({'_hidden': currentPage <= 1});
    const rightButtonClass = classnames({'_hidden': currentPage >= pageCount});
    return (
      <div className="fc-table-paginator">
        <LeftButton className={leftButtonClass} onClick={this.onPrevPageClick}/>
        <div className="fc-table-paginator__current-page">
          {this.currentPageSelector(pageCount)}
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
