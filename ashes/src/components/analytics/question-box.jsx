// @flow

// libs
import React, { PropTypes } from 'react';

// styles
import styles from './question-box.css';

const QuestionBox = props => {

  const { content, title, trendDisplay } = props;

  return (
    <div styleName="question-box-container">
      <div styleName="question-box-title">
        {title}
      </div>
      <div styleName="question-box-content">
        {content}
      </div>
      <div styleName="question-box-trend-display">
        {trendDisplay}
      </div>
    </div>
  );
};

QuestionBox.propTypes = {
  title: PropTypes.string,
  content: PropTypes.node,
  trendDisplay: PropTypes.node,
  onClick: PropTypes.func,
};

export default QuestionBox;
