// libs
import React, { Component, PropTypes } from 'react';
import s from './category-item.css';

export const CategoryItem = props => {
  const item = props.model;

  return (
    <div className={s.item} key={`${item.id}${item.prefix}`}>
      <div className={s.itemPrefix}>{item.prefix}</div>
      <div className={s.itemValue}>{item.text}</div>
    </div>
  );
};
