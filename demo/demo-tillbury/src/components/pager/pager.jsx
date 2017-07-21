/* @flow */

import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import Icon from 'ui/icon';

import styles from './pager.css';

type Props = {
  className?: string,
  total: number,
  from: number,
  size: number,
  setState: ({from: number}) => void,
};
type State = {
  currentPage: number,
  pagesCount: number,
};

class Pager extends Component {
  props: Props;
  state: State = {
    currentPage: this.getPage(this.props.from, this.props.size),
    pagesCount: this.getPagesCount(this.props.total, this.props.size),
  };

  componentWillReceiveProps(props: Props) {
    this.setState({
      currentPage: this.getPage(props.from, props.size),
      pagesCount: this.getPagesCount(props.total, props.size),
    });
  }

  get pageText() {
    const { currentPage, pagesCount } = this.state;

    return (
      <span>
        <span>{currentPage}</span>
        <span styleName="delimiter">of</span>
        <span>{pagesCount}</span>
      </span>
    );
  }

  getPage(from: number, size: number) {
    return Math.floor(from / size + 1);
  }

  getPagesCount(total: number, size: number) {
    return Math.ceil(total / size) || 1;
  }

  setPage(page: number) {
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

  render() {
    const blockClass = classNames(this.props.className, styles.pager, {
      [styles._hidden]: this.state.pagesCount == 1,
    });
    const leftArrowClass = classNames(styles['left-arrow'], {
      [styles._hidden]: this.state.currentPage == 1,
    });
    const rightArrowClass = classNames(styles['right-arrow'], {
      [styles._hidden]: this.state.currentPage == this.state.pagesCount,
    });
    return (
      <div className={blockClass}>
        <div className={leftArrowClass} onClick={this.onPrevPageClick}>
          <Icon name="fc-arrow-left" styleName="arrow-icon" />
        </div>
        <div styleName="page-text">{this.pageText}</div>
        <div className={rightArrowClass} onClick={this.onNextPageClick}>
          <Icon name="fc-arrow-right" styleName="arrow-icon" />
        </div>
      </div>
    );
  }
}

export default Pager;
