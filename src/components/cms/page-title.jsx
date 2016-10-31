/* @flow */

// libs
import React from 'react';

// styles
import styles from './cms.css';

type Props = {
  title: string,
};

const PageTitle = (props: Props) => {
  return (
    <div styleName="title-block">
      <h1 styleName="page-title">{props.title}</h1>
      <div styleName="divider"></div>
    </div>
  );
};

export default PageTitle;
