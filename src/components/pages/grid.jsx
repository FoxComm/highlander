
import React from 'react';
import styles from './css/grid.css';

const Grid = () => {
  return (
    <div styleName="body">
      <section styleName="content">
        <div styleName="picture"></div>
        <div styleName="text">
          LOREM IPSUM Nullam quis risus eget urna mollis ornare vel eu leo. Nulla vitae elit libero,
            a pharetra augue. Vivamus sagittis lacus vel augue laoreet rutrum
            faucibus dolor auctor. Cum sociis natoque penatibus et magnis dis
            parturient montes, nascetur ridiculus mus.
        </div>
      </section>
      <section styleName="grid">
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
        <div></div>
      </section>
    </div>
  );
};

export default Grid;
