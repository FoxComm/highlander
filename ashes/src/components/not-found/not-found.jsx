/* @flow */

// libs
import React from 'react';

// styles
import s from './not-found.css';

export default (props: Props) => (
  <div className={s.page}>
    <div className={s.logo} />
    <h1 className={s.title}>Page not found</h1>
  </div>
);
