//libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';

//helpers
import { prefix } from '../../lib/text-utils';

// components
import { LeftButton, RightButton } from 'components/core/button';
import { Lookup } from '../lookup';

import s from './paginator.css';

const prefixed = prefix('fc-table-paginator');

export default class TablePaginator extends React.Component {
  static propTypes = {
    total: PropTypes.number.isRequired,
    from: PropTypes.number.isRequired,
    size: PropTypes.number.isRequired,
    setState: PropTypes.func.isRequired
  };

  constructor(props, ...args) {
    super(props, ...args);

    this.state = {
      currentPage: this.getPage(props.from, props.size),
      pagesCount: this.getPagesCount(props.total, props.size),
    };
  }

  componentWillReceiveProps(props) {
    this.setState({
      currentPage: this.getPage(props.from, props.size),
      pagesCount: this.getPagesCount(props.total, props.size),
    });
  }

  getPage(from, size) {
    return Math.floor(from / size + 1);
  }

  getPagesCount(total, size) {
    return Math.ceil(total / size) || 1;
  }

  setPage(page) {
    const {size, setState} = this.props;

    setState({
      from: (page - 1) * size,
    });
  }

  @autobind
  onPrevPageClick() {
    this.setPage(this.state.currentPage - 1);
  }

  @autobind
  onNextPageClick() {
    this.setPage(this.state.currentPage + 1);
  }

  @autobind
  onSelect({id}) {
    this.setPage(id);
  }

  renderPagesCount() {
    return(
      <div className={prefixed('total-pages')}>
        {this.state.pagesCount}
      </div>
    );
  }

  renderCurrentPage() {
    const { currentPage, pagesCount } = this.state;

    if (pagesCount <= 1) return this.renderPagesCount();

    return (
      <Lookup className={prefixed('current-page')}
              data={_.range(1, pagesCount + 1).map(page => ({id: page, label: String(page)}))}
              value={currentPage}
              minQueryLength={0}
              onSelect={this.onSelect} />
    );
  }

  render() {
    const { currentPage, pagesCount } = this.state;

    const leftDisabled = currentPage <= 1;
    const rightDisabled = currentPage >= pagesCount;

    return (
      <div className={prefixed()}>
        <LeftButton
          onClick={this.onPrevPageClick}
          disabled={leftDisabled}
          className={s.paginatorButton}
        />
        {this.renderCurrentPage()}
        <div className={prefixed('separator')}>of</div>
        {this.renderPagesCount()}
        <RightButton
          onClick={this.onNextPageClick}
          disabled={rightDisabled}
          className={s.paginatorButton}
        />
      </div>
    );
  }
}
