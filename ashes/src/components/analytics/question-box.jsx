// @flow

// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// styles
import styles from './question-box.css';

// types
export type Props = {
  id: string,
  title: string,
  content: any,
  footer: any,
  isActive?: boolean,
  onClick: Function,
  isClickable?: boolean,
}

class QuestionBox extends React.Component {

  props: Props;

  static defaultProps = {
    isActive: false,
    onClick: _.noop,
    isClickable: true,
  };

  @autobind
  onClickHandler() {
    const { onClick, isActive, isClickable } = this.props;

    if (!_.isNil(onClick) && !isActive && isClickable) {
      return onClick(this.props);
    }

    return null;
  }

  render() {
    const { content, title, footer, isActive } = this.props;
    const isActiveStyle = isActive ? 'active' : 'inactive';

    return (
      <div styleName={`question-box-container-${isActiveStyle}`} onClick={this.onClickHandler}>
        <div styleName="question-box-title">
          {title}
        </div>
        <div styleName="question-box-content">
          {content}
        </div>
        <div styleName="question-box-footer-display">
          {footer}
        </div>
      </div>
    );
  }
}

export default QuestionBox;
