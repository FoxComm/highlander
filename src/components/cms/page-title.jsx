/* @flow */

// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// paragons
import { fieldTypes } from 'paragons/cms';

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
