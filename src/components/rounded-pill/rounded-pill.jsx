/**
 * @flow
 */

import React from 'react';

import styles from './rounded-pill.css';

type Props = {
  text: string,
  value: string,
  onClick: (value: string) => void,
};

const RoundedPill = (props: Props) => {
  return (
    <div styleName="main">
      <div styleName="text">{props.text}</div>
      <div>
        <button styleName="button" onClick={() => props.onClick(props.value)}>
          <i className="icon-close" />
        </button>
      </div>
    </div>
  );
};

export default RoundedPill;
