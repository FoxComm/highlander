// @flow

// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// styles
import styles from './question-box.css';

// types
type Props = {
  title: string,
  content: any,
  footer: any,
  onClick?: Function,
}

type State = {
  isActive: boolean,
}

export default class QuestionBox extends React.Component {

  props: Props;

  state: State = {
    isActive: false
  };

  @autobind
  onClickHandler() {
    const { onClick } = this.props;
    const { isActive } = this.state;

    this.setState({isActive: !isActive});

    if (!_.isNull(onClick)) {
      return onClick();
    }
    return null;
  }

  render() {
    const { content, title, footer } = this.props;
    const isActive = this.state.isActive ? 'active' : 'inactive';

    return (
      <div styleName={`question-box-container-${isActive}`} onClick={this.onClickHandler}>
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
