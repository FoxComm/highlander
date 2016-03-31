//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

//helpers
import { prefix } from '../../lib/text-utils';

// components
import { LeftButton, RightButton } from '../common/buttons';
import { Lookup } from '../lookup';


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
      pagesCount: this.getPage(props.total, props.size),
    };
  }

  componentWillReceiveProps(props) {
    this.setState({
      currentPage: this.getPage(props.from, props.size),
      pagesCount: this.getPage(props.total, props.size),
    });
  }

  getPage(from, size) {
    return Math.ceil(from / size + 1);
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

  render() {
    const { currentPage, pagesCount } = this.state;

    const leftButtonClass = classNames({'_hidden': currentPage <= 1});
    const rightButtonClass = classNames({'_hidden': currentPage >= pagesCount});

    return (
      <div className={prefixed()}>
        <LeftButton className={leftButtonClass} onClick={this.onPrevPageClick}/>
        <Lookup className={prefixed('current-page')}
                data={_.range(1, pagesCount + 1).map(page => ({id: page, label: String(page)}))}
                value={currentPage}
                minQueryLength={0}
                onSelect={this.onSelect}/>
        <div className={prefixed('separator')}>of</div>
        <div className={prefixed('total-pages')}>
          {pagesCount}
        </div>
        <RightButton className={rightButtonClass} onClick={this.onNextPageClick}/>
      </div>
    );
  }
}
